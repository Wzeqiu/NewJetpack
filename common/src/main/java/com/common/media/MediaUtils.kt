package com.common.media

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import kotlin.math.min

/**
 * 音视频工具类
 * 提供音视频文件的基本操作，如：获取媒体信息、裁剪媒体、提取音轨等功能
 */
object MediaUtils {

    private const val TAG = "MediaUtils"

    /**
     * 获取媒体文件信息
     * @param path 媒体文件路径
     * @return MediaInfo 媒体信息对象
     */
    @Throws(Exception::class)
    fun getMediaInfo(path: String): MediaInfo? = MediaMetadataRetriever().runCatching {
        setDataSource(path)

        // 获取媒体类型
        val mimeType = extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        val mediaType = when {
            mimeType?.contains("video") == true -> MediaConfig.MEDIA_TYPE_VIDEO
            mimeType?.contains("audio") == true -> MediaConfig.MEDIA_TYPE_AUDIO
            else -> MediaConfig.MEDIA_TYPE_IMAGE
        }

        // 获取媒体尺寸（宽高）
        val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toIntOrNull() ?: 0
        val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toIntOrNull() ?: 0

        // 获取媒体时长
        val duration = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull() ?: 0

        // 获取媒体文件信息
        val file = File(path)

        MediaInfo(
            name = file.name,
            path = path,
            size = file.length(),
            duration = duration,
            mediaType = mediaType,
            width = width,
            height = height
        )
    }.getOrElse {
        null
    }

    /**
     * 计算媒体文件MD5值
     * @param filePath 媒体文件路径
     * @return 文件MD5值，如果计算失败则返回空字符串
     */
    fun calculateMD5(filePath: String): String = runCatching {
        File(filePath).takeIf { it.exists() && it.isFile }?.let { file ->
            FileInputStream(file).use { fis ->
                val md = MessageDigest.getInstance("MD5")
                val buffer = ByteArray(8192)

                generateSequence { fis.read(buffer) }
                    .takeWhile { it > 0 }
                    .forEach { bytesRead -> md.update(buffer, 0, bytesRead) }

                BigInteger(1, md.digest()).toString(16).padStart(32, '0')
            }
        } ?: ""
    }.getOrElse {
        Log.e(TAG, "计算MD5时出错: ${it.message}")
        ""
    }

    /**
     * 裁剪音视频
     * @param context 上下文
     * @param sourceFilePath 源文件路径
     * @param outputFilePath 输出文件路径
     * @param startTimeMs 开始时间（毫秒）
     * @param endTimeMs 结束时间（毫秒），如果为-1则裁剪到文件末尾
     * @param callback 裁剪结果回调
     */
    @OptIn(UnstableApi::class)
    fun trimMedia(
        context: Context,
        sourceFilePath: String,
        outputFilePath: String,
        startTimeMs: Long,
        endTimeMs: Long,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        runCatching {
            val sourceFile = File(sourceFilePath).apply {
                if (!exists()) {
                    return callback(false, "源文件不存在")
                }
            }

            // 创建输出文件目录
            File(outputFilePath).apply {
                parentFile?.takeIf { !it.exists() }?.mkdirs()
                if (exists()) delete() // 如果输出文件已存在，则删除
            }

            val durationMs = getMediaDuration(sourceFilePath)
            val finalEndTimeMs =
                if (endTimeMs <= 0 || endTimeMs > durationMs) durationMs else endTimeMs

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(sourceFile))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startTimeMs)
                        .setEndPositionMs(finalEndTimeMs)
                        .build()
                ).build()

            // 创建裁剪效果
            val clippedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveAudio(false)
                .setRemoveVideo(false)
                .setFlattenForSlowMotion(false)
                .build()

            // 创建转换器
            val transformer = Transformer.Builder(context).build()
            val countDownLatch = CountDownLatch(1)
            var exportSuccess = false
            var errorMessage = ""

            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    exportSuccess = true
                    countDownLatch.countDown()
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    errorMessage = "导出失败: ${exception.message}"
                    countDownLatch.countDown()
                }
            })
            // 执行导出
            transformer.start(clippedMediaItem, outputFilePath)
            // 等待导出完成
            countDownLatch.await()

            if (exportSuccess) {
                callback(true, "媒体裁剪成功")
            } else {
                callback(false, errorMessage.ifEmpty { "媒体裁剪失败" })
            }
        }.onFailure { e ->
            Log.e(TAG, "裁剪媒体时出错: ${e.message}")
            callback(false, "裁剪出错: ${e.message}")
        }
    }

    /**
     * 提取音频
     * @param context 上下文
     * @param videoPath 视频文件路径
     * @param outputPath 输出的音频文件路径
     * @param callback 提取结果回调
     */
    @OptIn(UnstableApi::class)
    fun extractAudio(
        context: Context,
        videoPath: String,
        outputPath: String,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        runCatching {
            val videoFile = File(videoPath).apply {
                if (!exists()) {
                    return callback(false, "视频文件不存在")
                }
            }

            // 创建输出文件目录
            File(outputPath).apply {
                parentFile?.takeIf { !it.exists() }?.mkdirs()
                if (exists()) delete() // 如果输出文件已存在，则删除
            }

            val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))

            // 创建只包含音频的编辑项
            val audioOnlyItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true)  // 移除视频轨道，只保留音频
                .setRemoveAudio(false)
                .build()

            // 创建转换器
            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC) // 设置音频输出格式为AAC
                .build()

            val countDownLatch = CountDownLatch(1)
            var exportSuccess = false
            var errorMessage = ""

            transformer.addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    exportSuccess = true
                    countDownLatch.countDown()
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    errorMessage = "提取音频失败: ${exception.message}"
                    countDownLatch.countDown()
                }
            })
            Handler(Looper.getMainLooper()).post {
                // 执行导出
                transformer.start(audioOnlyItem, outputPath)
            }
            // 等待导出完成
            countDownLatch.await()

            if (exportSuccess) {
                callback(true, "音频提取成功")
            } else {
                callback(false, errorMessage.ifEmpty { "音频提取失败" })
            }
        }.onFailure { e ->
            Log.e(TAG, "提取音频时出错: ${e.message}")
            callback(false, "提取音频出错: ${e.message}")
        }
    }

    /**
     * 修改音视频文件名
     * @param filePath 原文件路径
     * @param newName 新文件名（不包含路径和扩展名）
     * @return 修改后的文件路径，如果修改失败则返回原路径
     */
    fun renameMediaFile(filePath: String, newName: String): String = runCatching {
        val file = File(filePath).takeIf { it.exists() } ?: return@runCatching filePath

        val extension = file.extension
        val parentPath = file.parent ?: return@runCatching filePath
        val newFile = File(parentPath, "$newName.$extension")

        // 如果新文件已存在，则返回原路径
        if (newFile.exists()) {
            return@runCatching filePath
        }

        // 重命名文件
        if (file.renameTo(newFile)) {
            newFile.absolutePath
        } else {
            filePath
        }
    }.getOrElse { e ->
        Log.e(TAG, "重命名文件时出错: ${e.message}")
        filePath
    }

    /**
     * 获取媒体文件时长（毫秒）
     * @param filePath 媒体文件路径
     * @return 媒体时长，如果获取失败则返回0
     */
    fun getMediaDuration(filePath: String): Long = MediaMetadataRetriever().runCatching {
        setDataSource(filePath)
        val duration =
            extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        release()
        duration
    }.getOrElse { e ->
        Log.e(TAG, "获取媒体时长时出错: ${e.message}")
        0L
    }

    /**
     * 获取视频文件的宽高
     * @param filePath 视频文件路径
     * @return Pair<宽, 高>，如果获取失败则返回Pair(0, 0)
     */
    fun getVideoResolution(filePath: String): Pair<Int, Int> =
        MediaMetadataRetriever().runCatching {
            setDataSource(filePath)
            val width = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val height = extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0
            release()
            Pair(width, height)
        }.getOrElse { e ->
            Log.e(TAG, "获取视频分辨率时出错: ${e.message}")
            Pair(0, 0)
        }


    /**
     * 获取视频关键帧的时间点列表（毫秒）
     * @param context 上下文
     * @param videoPath 视频文件路径
     * @return 关键帧时间点列表（毫秒）
     */
    @OptIn(UnstableApi::class)
    fun getVideoKeyFramePositions(context: Context, videoPath: String): List<Long> =
        MediaExtractor().runCatching {
            val keyFramePositions = mutableListOf<Long>()
            setDataSource(videoPath)
            // 查找视频轨道
            val videoTrackIndex = (0 until trackCount)
                .firstOrNull { i ->
                    val format = getTrackFormat(i)
                    format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                } ?: -1

            if (videoTrackIndex >= 0) {
                selectTrack(videoTrackIndex)
                // 采样查找关键帧
                val buffer = ByteBuffer.allocate(1024 * 1024)  // 1MB缓冲区
                while (readSampleData(buffer, 0) >= 0) {
                    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        // 这是一个关键帧
                        keyFramePositions.add(sampleTime / 1000)  // 转换为毫秒
                    }
                    advance()
                }
            }
            keyFramePositions
        }.getOrElse { e ->
            Log.e(TAG, "获取视频关键帧时出错: ${e.message}")
            mutableListOf()
        }

    /**
     * 检查文件是否为音频文件
     * @param filePath 文件路径
     * @return 是否为音频文件
     */
    fun isAudioFile(filePath: String): Boolean = MediaExtractor().runCatching {
        setDataSource(filePath)
        (0 until trackCount).any { i ->
            val format = getTrackFormat(i)
            format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
    }.getOrElse { e ->
        Log.e(TAG, "检查音频文件时出错: ${e.message}")
        false
    }


    /**
     * 检查文件是否为视频文件
     * @param filePath 文件路径
     * @return 是否为视频文件
     */
    fun isVideoFile(filePath: String): Boolean = MediaExtractor().runCatching {
        setDataSource(filePath)
        (0 until trackCount).any { i ->
            val format = getTrackFormat(i)
            format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        }
    }.getOrElse { e ->
        Log.e(TAG, "检查视频文件时出错: ${e.message}")
        false
    }
} 