package com.common.media

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 媒体处理器
 * 提供简化的媒体处理API，并支持协程
 */
class MediaProcessor private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: MediaProcessor?=null

        /**
         * 获取媒体处理器实例
         * @param context 上下文
         * @return MediaProcessor实例
         */
        fun getInstance(context: Context): MediaProcessor {
            return instance ?: synchronized(this) {
                instance ?: MediaProcessor(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 获取媒体信息
     * @param path 媒体文件路径
     * @return 媒体信息对象，如果获取失败则返回null
     */
    suspend fun getMediaInfo(path: String): MediaInfo? =
        suspendCancellableCoroutine { continuation ->
            runCatching {
                MediaUtils.getMediaInfo(path)
            }.fold(
                onSuccess = { mediaInfo ->
                    if (continuation.isActive) {
                        continuation.resume(mediaInfo)
                    }
                },
                onFailure = {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            )
        }

    /**
     * 计算媒体文件MD5
     * @param path 媒体文件路径
     * @return 文件MD5值
     */
    suspend fun calculateMD5(path: String): String = suspendCancellableCoroutine { continuation ->
        val md5 = MediaUtils.calculateMD5(path)
        if (continuation.isActive) {
            continuation.resume(md5)
        }
    }

    /**
     * 裁剪媒体文件
     * @param sourcePath 源文件路径
     * @param outputPath 输出文件路径
     * @param startTimeMs 开始时间（毫秒）
     * @param endTimeMs 结束时间（毫秒），如果为-1则裁剪到文件末尾
     * @return 裁剪结果，成功返回true，否则抛出异常
     */
    suspend fun trimMedia(
        sourcePath: String,
        outputPath: String,
        startTimeMs: Long,
        endTimeMs: Long = -1
    ): Boolean = suspendCancellableCoroutine { continuation ->
        MediaUtils.trimMedia(
            context,
            sourcePath,
            outputPath,
            startTimeMs,
            endTimeMs
        ) { success, message ->
            if (continuation.isActive) {
                if (success) {
                    continuation.resume(true)
                } else {
                    File(outputPath).apply {
                        if (exists()) delete()
                    }
                    continuation.resumeWithException(Exception(message))
                }
            }
        }
        continuation.invokeOnCancellation {
            // 处理协程取消的情况，如有需要可以在这里添加清理代码
            File(outputPath).apply {
                if (exists()) delete()
            }
        }
    }

    /**
     * 提取音频
     * @param videoPath 视频文件路径
     * @param outputPath 输出音频文件路径
     * @return 提取结果，成功返回true，否则抛出异常
     */
    suspend fun extractAudio(
        videoPath: String,
        outputPath: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        MediaUtils.extractAudio(context, videoPath, outputPath) { success, message ->
            if (continuation.isActive) {
                if (success) {
                    continuation.resume(true)
                } else {
                    File(outputPath).apply {
                        if (exists()) delete()
                    }
                    continuation.resumeWithException(Exception(message))
                }
            }
        }
        continuation.invokeOnCancellation {
            // 处理协程取消的情况
            File(outputPath).apply {
                if (exists()) delete()
            }
        }
    }

    /**
     * 重命名媒体文件
     * @param filePath 原文件路径
     * @param newName 新文件名（不包含路径和扩展名）
     * @return 修改后的文件路径
     */
    suspend fun renameMediaFile(filePath: String, newName: String): String =
        suspendCancellableCoroutine { continuation ->
            val newPath = MediaUtils.renameMediaFile(filePath, newName)
            if (continuation.isActive) {
                continuation.resume(newPath)
            }
        }

    /**
     * 获取媒体文件时长（毫秒）
     * @param filePath 媒体文件路径
     * @return 媒体时长
     */
    suspend fun getMediaDuration(filePath: String): Long =
        suspendCancellableCoroutine { continuation ->
            val duration = MediaUtils.getMediaDuration(filePath)
            if (continuation.isActive) {
                continuation.resume(duration)
            }
        }

    /**
     * 获取视频文件的宽高
     * @param filePath 视频文件路径
     * @return Pair(宽, 高)
     */
    suspend fun getVideoResolution(filePath: String): Pair<Int, Int> =
        suspendCancellableCoroutine { continuation ->
            val resolution = MediaUtils.getVideoResolution(filePath)
            if (continuation.isActive) {
                continuation.resume(resolution)
            }
        }

    /**
     * 检查文件是否为音频文件
     * @param filePath 文件路径
     * @return 是否为音频文件
     */
    suspend fun isAudioFile(filePath: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val isAudio = MediaUtils.isAudioFile(filePath)
            if (continuation.isActive) {
                continuation.resume(isAudio)
            }
        }

    /**
     * 检查文件是否为视频文件
     * @param filePath 文件路径
     * @return 是否为视频文件
     */
    suspend fun isVideoFile(filePath: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val isVideo = MediaUtils.isVideoFile(filePath)
            if (continuation.isActive) {
                continuation.resume(isVideo)
            }
        }
} 