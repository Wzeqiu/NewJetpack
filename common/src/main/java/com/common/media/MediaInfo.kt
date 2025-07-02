package com.common.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * 媒体信息数据类
 * 用于存储和传递媒体文件(图片、视频、音频)的相关信息
 */
@Parcelize
data class MediaInfo(
    /** 媒体文件名称 */
    val name: String,
    
    /** 媒体文件路径，可能会在压缩后被修改 */
    var path: String,
    
    /** 媒体文件大小，单位为字节(byte) */
    val size: Long = 0,
    
    /** 媒体文件时长，仅适用于音频和视频，单位为毫秒(ms) */
    val duration: Long = 0,
    
    /** 
     * 媒体类型
     * @see MediaConfig.MEDIA_TYPE_IMAGE
     * @see MediaConfig.MEDIA_TYPE_VIDEO
     * @see MediaConfig.MEDIA_TYPE_AUDIO
     */
    val mediaType: Int = -1,
    
    /** 媒体文件宽度，仅适用于图片和视频，单位为像素(px) */
    val width: Int = 0,
    
    /** 媒体文件高度，仅适用于图片和视频，单位为像素(px) */
    val height: Int = 0,
    
    /** 是否被选中，用于多选场景 */
    var isSelect: Boolean = false,
) : Parcelable {
    
    /**
     * 获取媒体文件的扩展名
     * @return 文件扩展名，如 "jpg", "mp4", "mp3" 等，如果没有扩展名则返回空字符串
     */
    fun getExtension(): String {
        return path.substringAfterLast('.', "")
    }
    
    /**
     * 检查媒体文件是否存在
     * @return 文件是否存在
     */
    fun exists(): Boolean {
        return File(path).exists()
    }
    
    /**
     * 获取格式化的文件大小
     * @return 格式化后的文件大小，如 "1.5MB", "900KB" 等
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.1f", size / 1024f)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format("%.1f", size / (1024f * 1024f))} MB"
            else -> "${String.format("%.1f", size / (1024f * 1024f * 1024f))} GB"
        }
    }
    
    /**
     * 获取格式化的媒体时长
     * @return 格式化后的时长，如 "01:30", "02:45:30" 等，仅适用于音频和视频
     */
    fun getFormattedDuration(): String {
        if (duration <= 0) return "00:00"
        
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 判断是否为图片类型
     */
    fun isImage(): Boolean = mediaType == MediaConfig.MEDIA_TYPE_IMAGE
    
    /**
     * 判断是否为视频类型
     */
    fun isVideo(): Boolean = mediaType == MediaConfig.MEDIA_TYPE_VIDEO
    
    /**
     * 判断是否为音频类型
     */
    fun isAudio(): Boolean = mediaType == MediaConfig.MEDIA_TYPE_AUDIO
}
