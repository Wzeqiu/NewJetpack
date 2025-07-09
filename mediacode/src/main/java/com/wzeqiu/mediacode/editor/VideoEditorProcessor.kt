package com.wzeqiu.mediacode.editor

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * 视频编辑处理器
 *
 * 提供视频处理相关功能，包括：
 * - 视频裁剪
 * - 视频分割
 * - 视频拼接
 * - 视频旋转与翻转
 * - 视频变速
 * - 添加滤镜和特效
 * - 提取视频帧
 */
class VideoEditorProcessor private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VideoEditorProcessor"

        @Volatile
        private var instance: VideoEditorProcessor? = null

        /**
         * 获取视频编辑处理器实例
         */
        fun getInstance(context: Context): VideoEditorProcessor {
            return instance ?: synchronized(this) {
                instance ?: VideoEditorProcessor(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * 视频裁剪
     *
     * @param sourcePath 源视频路径
     * @param outputPath 输出视频路径
     * @param startMs 开始时间（毫秒）
     * @param endMs 结束时间（毫秒）
     * @return 处理结果，包括成功/失败状态和输出路径
     */
    @OptIn(UnstableApi::class)
    suspend fun trimVideo(
        sourcePath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                continuation.resume(ProcessResult.Error("源视频文件不存在"))
                return@suspendCancellableCoroutine
            }

            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            // 创建MediaItem并设置裁剪范围
            val mediaItem = MediaItem.Builder()
                .setUri(Uri.fromFile(sourceFile))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()

            // 创建编辑后的媒体项
            val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

            // 创建转换器
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        result: ExportResult
                    ) {
                        continuation.resume(ProcessResult.Success(outputPath))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        continuation.resume(ProcessResult.Error("视频裁剪失败: ${exception.message}"))
                    }
                })
                .build()

            // 开始转换
            transformer.start(editedMediaItem, outputPath)

            // 注册取消回调
            continuation.invokeOnCancellation {
                // 尝试清理资源
                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "取消处理时清理资源失败", e)
                }
            }
        } catch (e: Exception) {
            continuation.resume(ProcessResult.Error("视频裁剪异常: ${e.message}"))
        }
    }

    /**
     * 旋转和翻转视频
     *
     * @param sourcePath 源视频路径
     * @param outputPath 输出视频路径
     * @param rotationDegrees 旋转角度（度）
     * @param flipHorizontal 是否水平翻转
     * @param flipVertical 是否垂直翻转
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun rotateAndFlipVideo(
        sourcePath: String,
        outputPath: String,
        rotationDegrees: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                continuation.resume(ProcessResult.Error("源视频文件不存在"))
                return@suspendCancellableCoroutine
            }

            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            // 计算缩放因子
            val scaleX = if (flipHorizontal) -1f else 1f
            val scaleY = if (flipVertical) -1f else 1f

            // 创建旋转和翻转变换
            val transformation = ScaleAndRotateTransformation.Builder()
                .setRotationDegrees(rotationDegrees)
                .setScale(scaleX, scaleY)
                .build()

            // 创建MediaItem
            val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))
            val effects = Effects(listOf(/* audioProcessors */), listOf(transformation))
            // 创建编辑后的媒体项并应用变换
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            // 创建转换器
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        result: ExportResult
                    ) {
                        continuation.resume(ProcessResult.Success(outputPath))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        continuation.resume(ProcessResult.Error("视频处理失败: ${exception.message}"))
                    }
                })
                .build()

            // 开始转换
            transformer.start(editedMediaItem, outputPath)

            // 注册取消回调
            continuation.invokeOnCancellation {
                // 尝试清理资源
                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "取消处理时清理资源失败", e)
                }
            }
        } catch (e: Exception) {
            continuation.resume(ProcessResult.Error("视频处理异常: ${e.message}"))
        }
    }

    /**
     * 变速视频
     *
     * @param sourcePath 源视频路径
     * @param outputPath 输出视频路径
     * @param speed 变速因子（0.25-4.0，小于1表示慢放，大于1表示快放）
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun changeVideoSpeed(
        sourcePath: String,
        outputPath: String,
        speed: Float
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                continuation.resume(ProcessResult.Error("源视频文件不存在"))
                return@suspendCancellableCoroutine
            }

            // 限制速度范围
            val limitedSpeed = speed.coerceIn(0.25f, 4.0f)

            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            // 创建变速效果
            val speedEffect = SpeedChangeEffect(limitedSpeed)

            // 创建MediaItem
            val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))

            val effects = Effects(listOf(/* audioProcessors */), listOf(speedEffect))
            // 创建编辑后的媒体项并应用变速效果
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setEffects(effects)
                .build()

            // 创建转换器
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        result: ExportResult
                    ) {
                        continuation.resume(ProcessResult.Success(outputPath))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        continuation.resume(ProcessResult.Error("视频变速失败: ${exception.message}"))
                    }
                })
                .build()

            // 开始转换
            transformer.start(editedMediaItem, outputPath)

            // 注册取消回调
            continuation.invokeOnCancellation {
                // 尝试清理资源
                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "取消处理时清理资源失败", e)
                }
            }
        } catch (e: Exception) {
            continuation.resume(ProcessResult.Error("视频变速异常: ${e.message}"))
        }
    }

    /**
     * 拼接多个视频
     *
     * @param videoPaths 源视频路径列表
     * @param outputPath 输出视频路径
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun mergeVideos(
        videoPaths: List<String>,
        outputPath: String
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            if (videoPaths.isEmpty()) {
                continuation.resume(ProcessResult.Error("没有指定要合并的视频"))
                return@suspendCancellableCoroutine
            }

            // 检查所有源文件是否存在
            for (path in videoPaths) {
                val file = File(path)
                if (!file.exists()) {
                    continuation.resume(ProcessResult.Error("源视频文件不存在: $path"))
                    return@suspendCancellableCoroutine
                }
            }

            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()


            val mediaItemSequence = EditedMediaItemSequence.Builder()
            // 创建多个MediaItem
            videoPaths.map { path ->
                mediaItemSequence.addItem(
                    EditedMediaItem.Builder(
                        MediaItem.fromUri(
                            Uri.fromFile(
                                File(path)
                            )
                        )
                    ).build()
                )
            }

            // 创建Composition
            val composition = Composition.Builder(mediaItemSequence.build()).build()


            // 创建转换器
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        result: ExportResult
                    ) {
                        continuation.resume(ProcessResult.Success(outputPath))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        continuation.resume(ProcessResult.Error("视频合并失败: ${exception.message}"))
                    }
                })
                .build()

            // 开始转换
            transformer.start(composition, outputPath)

            // 注册取消回调
            continuation.invokeOnCancellation {
                // 尝试清理资源
                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "取消处理时清理资源失败", e)
                }
            }
        } catch (e: Exception) {
            continuation.resume(ProcessResult.Error("视频合并异常: ${e.message}"))
        }
    }

    /**
     * 提取音频
     *
     * @param videoPath 视频文件路径
     * @param outputPath 输出音频路径
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun extractAudio(
        videoPath: String,
        outputPath: String
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            val videoFile = File(videoPath)
            if (!videoFile.exists()) {
                continuation.resume(ProcessResult.Error("视频文件不存在"))
                return@suspendCancellableCoroutine
            }

            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()

            // 创建MediaItem
            val mediaItem = MediaItem.fromUri(Uri.fromFile(videoFile))

            // 创建编辑后的媒体项，设置为仅音频
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true) // 移除视频轨道，只保留音频
                .build()

            // 创建转换器
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        result: ExportResult
                    ) {
                        continuation.resume(ProcessResult.Success(outputPath))
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        continuation.resume(ProcessResult.Error("提取音频失败: ${exception.message}"))
                    }
                })
                .build()

            // 开始转换
            transformer.start(editedMediaItem, outputPath)

            // 注册取消回调
            continuation.invokeOnCancellation {
                // 尝试清理资源
                try {
                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "取消处理时清理资源失败", e)
                }
            }
        } catch (e: Exception) {
            continuation.resume(ProcessResult.Error("提取音频异常: ${e.message}"))
        }
    }

    /**
     * 获取视频时长
     *
     * @param videoPath 视频文件路径
     * @return 视频时长（毫秒），如果获取失败则返回0
     */
    fun getVideoDuration(videoPath: String): Long {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(videoPath)
            var duration = 0L

            // 查找视频轨道
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime?.startsWith("video/") == true) {
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // 微秒转毫秒
                        break
                    }
                }
            }

            duration
        } catch (e: Exception) {
            Log.e(TAG, "获取视频时长失败", e)
            0L
        } finally {
            extractor.release()
        }
    }

    /**
     * 获取视频信息
     *
     * @param videoPath 视频文件路径
     * @return 媒体信息对象，如果获取失败则返回null
     */
    fun getVideoInfo(videoPath: String): MediaInfo? {
        return try {
            val file = File(videoPath)
            if (!file.exists()) return null

            val extractor = MediaExtractor()
            extractor.setDataSource(videoPath)

            var width = 0
            var height = 0
            var duration = 0L

            // 查找视频轨道
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime?.startsWith("video/") == true) {
                    if (format.containsKey(MediaFormat.KEY_WIDTH)) {
                        width = format.getInteger(MediaFormat.KEY_WIDTH)
                    }
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) {
                        height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    }
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // 微秒转毫秒
                    }
                    break
                }
            }

            extractor.release()

            MediaInfo(
                name = file.name,
                path = videoPath,
                size = file.length(),
                duration = duration,
                mediaType = MediaConfig.MEDIA_TYPE_VIDEO,
                width = width,
                height = height
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取视频信息失败", e)
            null
        }
    }
}

/**
 * 处理结果密封类
 */
sealed class ProcessResult {
    /**
     * 处理成功
     *
     * @param outputPath 输出文件路径
     */
    data class Success(val outputPath: String) : ProcessResult()

    /**
     * 处理失败
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : ProcessResult()
} 