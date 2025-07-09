package com.wzeqiu.mediacode.editor

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.VolumeEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * 音频编辑处理器
 * 
 * 提供音频处理相关功能，包括：
 * - 音频裁剪
 * - 音频提取
 * - 音频混合
 * - 音量调节
 */
class AudioEditorProcessor private constructor(private val context: Context) {

    companion object {
        private const val TAG = "AudioEditorProcessor"
        
        @Volatile
        private var instance: AudioEditorProcessor? = null
        
        /**
         * 获取音频编辑处理器实例
         */
        fun getInstance(context: Context): AudioEditorProcessor {
            return instance ?: synchronized(this) {
                instance ?: AudioEditorProcessor(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 音频裁剪
     * 
     * @param sourcePath 源音频路径
     * @param outputPath 输出音频路径
     * @param startMs 开始时间（毫秒）
     * @param endMs 结束时间（毫秒）
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun trimAudio(
        sourcePath: String,
        outputPath: String,
        startMs: Long,
        endMs: Long
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                continuation.resume(ProcessResult.Error("源音频文件不存在"))
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
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true) // 确保移除可能存在的视频轨道
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
                        continuation.resume(ProcessResult.Error("音频裁剪失败: ${exception.message}"))
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
            continuation.resume(ProcessResult.Error("音频裁剪异常: ${e.message}"))
        }
    }
    
    /**
     * 调整音频音量
     * 
     * @param sourcePath 源音频路径
     * @param outputPath 输出音频路径
     * @param volume 音量因子（0.0-1.0）
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun adjustAudioVolume(
        sourcePath: String,
        outputPath: String,
        volume: Float
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                continuation.resume(ProcessResult.Error("源音频文件不存在"))
                return@suspendCancellableCoroutine
            }
            
            // 限制音量范围
            val limitedVolume = volume.coerceIn(0f, 1f)
            
            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // 创建音量效果
            val volumeEffect = VolumeEffect(limitedVolume)
            
            // 创建MediaItem
            val mediaItem = MediaItem.fromUri(Uri.fromFile(sourceFile))
            
            // 创建编辑后的媒体项并应用音量效果
            val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                .setRemoveVideo(true) // 确保移除可能存在的视频轨道
                .setEffects(listOf(volumeEffect))
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
                        continuation.resume(ProcessResult.Error("调整音频音量失败: ${exception.message}"))
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
            continuation.resume(ProcessResult.Error("调整音频音量异常: ${e.message}"))
        }
    }
    
    /**
     * 混合音频
     * 
     * @param audioPaths 源音频路径列表
     * @param outputPath 输出音频路径
     * @return 处理结果
     */
    @OptIn(UnstableApi::class)
    suspend fun mixAudios(
        audioPaths: List<String>,
        outputPath: String
    ): ProcessResult = suspendCancellableCoroutine { continuation ->
        try {
            // 检查输入参数
            if (audioPaths.isEmpty()) {
                continuation.resume(ProcessResult.Error("没有指定要混合的音频"))
                return@suspendCancellableCoroutine
            }
            
            // 检查所有源文件是否存在
            for (path in audioPaths) {
                val file = File(path)
                if (!file.exists()) {
                    continuation.resume(ProcessResult.Error("源音频文件不存在: $path"))
                    return@suspendCancellableCoroutine
                }
            }
            
            // 创建输出文件目录
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // 注意：由于Media3不直接支持音频混合，这里采用拼接方式
            // 在实际应用中，可能需要使用MediaCodec或FFmpeg等库来实现真正的音频混合
            
            // 创建多个MediaItem
            val mediaItems = audioPaths.map { path ->
                MediaItem.fromUri(Uri.fromFile(File(path)))
            }
            
            // 创建Composition
            val composition = Composition.Builder(mediaItems)
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
                        continuation.resume(ProcessResult.Error("音频混合失败: ${exception.message}"))
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
            continuation.resume(ProcessResult.Error("音频混合异常: ${e.message}"))
        }
    }
    
    /**
     * 获取音频时长
     * 
     * @param audioPath 音频文件路径
     * @return 音频时长（毫秒），如果获取失败则返回0
     */
    fun getAudioDuration(audioPath: String): Long {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(audioPath)
            var duration = 0L
            
            // 查找音频轨道
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                
                if (mime?.startsWith("audio/") == true) {
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // 微秒转毫秒
                        break
                    }
                }
            }
            
            duration
        } catch (e: Exception) {
            Log.e(TAG, "获取音频时长失败", e)
            0L
        } finally {
            extractor.release()
        }
    }
    
    /**
     * 获取音频信息
     * 
     * @param audioPath 音频文件路径
     * @return 媒体信息对象，如果获取失败则返回null
     */
    fun getAudioInfo(audioPath: String): MediaInfo? {
        return try {
            val file = File(audioPath)
            if (!file.exists()) return null
            
            val extractor = MediaExtractor()
            extractor.setDataSource(audioPath)
            
            var duration = 0L
            var sampleRate = 0
            var channelCount = 0
            
            // 查找音频轨道
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                
                if (mime?.startsWith("audio/") == true) {
                    if (format.containsKey(MediaFormat.KEY_DURATION)) {
                        duration = format.getLong(MediaFormat.KEY_DURATION) / 1000 // 微秒转毫秒
                    }
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    break
                }
            }
            
            extractor.release()
            
            MediaInfo(
                name = file.name,
                path = audioPath,
                size = file.length(),
                duration = duration,
                mediaType = MediaConfig.MEDIA_TYPE_AUDIO,
                width = 0,
                height = 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取音频信息失败", e)
            null
        }
    }
} 