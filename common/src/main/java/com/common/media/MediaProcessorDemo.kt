package com.common.media

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 媒体处理器演示类
 * 提供各种音视频处理操作的示例用法（基于协程）
 */
object MediaProcessorDemo {
    private const val TAG = "MediaProcessorDemo"

    /**
     * 展示如何获取媒体信息（使用协程）
     */
    fun showMediaInfo(context: Context, mediaPath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val processor = MediaProcessor.getInstance(context)
            
            runCatching {
                withContext(Dispatchers.IO) {
                    processor.getMediaInfo(mediaPath)
                }
            }.fold(
                onSuccess = { mediaInfo ->
                    if (mediaInfo != null) {
                        Log.d(TAG, "媒体信息:")
                        Log.d(TAG, "名称: ${mediaInfo.name}")
                        Log.d(TAG, "路径: ${mediaInfo.path}")
                        Log.d(TAG, "大小: ${mediaInfo.getFormattedSize()}")
                        Log.d(TAG, "类型: ${
                            when (mediaInfo.mediaType) {
                                MediaConfig.MEDIA_TYPE_IMAGE -> "图片"
                                MediaConfig.MEDIA_TYPE_VIDEO -> "视频"
                                MediaConfig.MEDIA_TYPE_AUDIO -> "音频"
                                else -> "未知"
                            }
                        }")
                        
                        if (mediaInfo.isVideo() || mediaInfo.isAudio()) {
                            Log.d(TAG, "时长: ${mediaInfo.getFormattedDuration()}")
                        }
                        
                        if (mediaInfo.isVideo() || mediaInfo.isImage()) {
                            Log.d(TAG, "分辨率: ${mediaInfo.width} x ${mediaInfo.height}")
                        }
                    } else {
                        Log.e(TAG, "无法获取媒体信息")
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "获取媒体信息出错: ${e.message}")
                }
            )
        }
    }
    
    /**
     * 展示如何裁剪视频（使用协程）
     */
    fun trimVideo(context: Context, videoPath: String, outputPath: String, startTimeMs: Long, durationMs: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            val processor = MediaProcessor.getInstance(context)
            
            runCatching {
                // 首先获取视频总时长
                val totalDuration = withContext(Dispatchers.IO) {
                    processor.getMediaDuration(videoPath)
                }
                Log.d(TAG, "视频总时长: ${formatDuration(totalDuration)}")
                
                // 确定结束时间点
                val endTimeMs = if (durationMs > 0) {
                    startTimeMs + durationMs
                } else {
                    -1L // 裁剪到结尾
                }
                
                // 开始裁剪
                val success = withContext(Dispatchers.IO) {
                    processor.trimMedia(videoPath, outputPath, startTimeMs, endTimeMs)
                }
                
                Log.d(TAG, "视频裁剪成功: $outputPath")
                
                // 获取裁剪后的视频信息
                val trimmedInfo = withContext(Dispatchers.IO) {
                    processor.getMediaInfo(outputPath)
                }
                if (trimmedInfo != null) {
                    Log.d(TAG, "裁剪后视频时长: ${trimmedInfo.getFormattedDuration()}")
                }
            }.onFailure { e ->
                Log.e(TAG, "视频裁剪出错: ${e.message}")
            }
        }
    }
    
    /**
     * 展示如何从视频中提取音频（使用协程）
     */
    fun extractAudioFromVideo(context: Context, videoPath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val processor = MediaProcessor.getInstance(context)
            
            runCatching {
                // 创建输出音频文件路径
                val videoFile = File(videoPath)
                val outputDir = File(videoFile.parent, "extracted_audio")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }
                
                val outputPath = File(outputDir, "${videoFile.nameWithoutExtension}.mp3").absolutePath
                
                // 提取音频
                val success = withContext(Dispatchers.IO) {
                    processor.extractAudio(videoPath, outputPath)
                }
                
                Log.d(TAG, "音频提取成功: $outputPath")
                
                // 获取提取的音频信息
                val audioInfo = withContext(Dispatchers.IO) {
                    processor.getMediaInfo(outputPath)
                }
                if (audioInfo != null) {
                    Log.d(TAG, "提取的音频时长: ${audioInfo.getFormattedDuration()}")
                    Log.d(TAG, "音频文件大小: ${audioInfo.getFormattedSize()}")
                }
            }.onFailure { e ->
                Log.e(TAG, "提取音频出错: ${e.message}")
            }
        }
    }
    
    /**
     * 展示如何计算媒体文件的MD5值（使用协程）
     */
    fun calculateFileMD5(context: Context, filePath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val processor = MediaProcessor.getInstance(context)
            
            runCatching {
                val md5 = withContext(Dispatchers.IO) {
                    processor.calculateMD5(filePath)
                }
                
                if (md5.isNotEmpty()) {
                    Log.d(TAG, "文件MD5: $md5")
                } else {
                    Log.e(TAG, "无法计算文件MD5")
                }
            }.onFailure { e ->
                Log.e(TAG, "计算MD5出错: ${e.message}")
            }
        }
    }
    
    /**
     * 展示如何重命名媒体文件（使用协程）
     */
    fun renameMediaFile(context: Context, filePath: String, newName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val processor = MediaProcessor.getInstance(context)
            
            runCatching {
                val newPath = withContext(Dispatchers.IO) {
                    processor.renameMediaFile(filePath, newName)
                }
                
                if (newPath != filePath) {
                    Log.d(TAG, "文件重命名成功: $newPath")
                } else {
                    Log.e(TAG, "文件重命名失败")
                }
            }.onFailure { e ->
                Log.e(TAG, "重命名文件出错: ${e.message}")
            }
        }
    }
    
    /**
     * 工具方法：格式化时长
     */
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
} 