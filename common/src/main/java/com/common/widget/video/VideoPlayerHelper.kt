package com.common.widget.video

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

/**
 * 视频播放器辅助类
 * 提供使用示例和常用功能封装
 */
object VideoPlayerHelper {
    private const val TAG = "VideoPlayerHelper"
    
    /**
     * 播放网络视频
     * @param videoView 自定义视频播放器视图
     * @param url 视频URL
     * @param autoPlay 是否自动播放
     * @param listener 播放监听器
     */
    fun playNetworkVideo(
        videoView: CustomVideoPlayerView,
        url: String,
        autoPlay: Boolean = true,
        listener: CustomVideoPlayerView.VideoPlayerListener? = null
    ) {
        setupVideoPlayer(videoView, listener)
        videoView.setVideoPath(url, autoPlay)
    }
    
    /**
     * 播放本地视频
     * @param videoView 自定义视频播放器视图
     * @param uri 视频URI
     * @param autoPlay 是否自动播放
     * @param listener 播放监听器
     */
    fun playLocalVideo(
        videoView: CustomVideoPlayerView,
        uri: Uri,
        autoPlay: Boolean = true,
        listener: CustomVideoPlayerView.VideoPlayerListener? = null
    ) {
        setupVideoPlayer(videoView, listener)
        videoView.setVideoUri(uri, autoPlay)
    }
    
    /**
     * 设置视频播放器
     * @param videoView 自定义视频播放器视图
     * @param listener 播放监听器
     */
    private fun setupVideoPlayer(
        videoView: CustomVideoPlayerView,
        listener: CustomVideoPlayerView.VideoPlayerListener?
    ) {
        // 设置圆角
        videoView.setCornerRadius(16f)
        
        // 设置调整大小模式为适应
        videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
        
        // 显示控制器
        videoView.setShowController(true)
        
        // 设置监听器
        if (listener != null) {
            videoView.setVideoPlayerListener(listener)
        } else {
            // 设置默认监听器
            videoView.setVideoPlayerListener(object : CustomVideoPlayerView.VideoPlayerListener {
                override fun onReady() {
                    Log.d(TAG, "视频准备就绪")
                }
                
                override fun onBuffering() {
                    Log.d(TAG, "视频缓冲中")
                }
                
                override fun onPlayingStateChanged(isPlaying: Boolean) {
                    Log.d(TAG, "播放状态: ${if (isPlaying) "播放中" else "已暂停"}")
                }
                
                override fun onCompletion() {
                    Log.d(TAG, "视频播放完成")
                }
                
                override fun onVideoDurationReady(durationMs: Long) {
                    Log.d(TAG, "视频时长: ${formatDuration(durationMs)}")
                }
                
                override fun onError(error: PlaybackException) {
                    Log.e(TAG, "播放错误: ${error.message}")
                }
            })
        }
    }
    
    /**
     * 格式化时长
     * @param durationMs 时长（毫秒）
     * @return 格式化后的时长字符串 (mm:ss)
     */
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * 清除视频缓存
     * @param context 上下文
     */
    fun clearVideoCache(context: Context) {
        VideoCacheManager.clearCache()
    }
    
    /**
     * 获取视频缓存大小
     * @return 缓存大小（字节）
     */
    fun getVideoCacheSize(): String {
        val cacheSize = VideoCacheManager.getCacheSize()
        return formatFileSize(cacheSize)
    }
    
    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的文件大小字符串
     */
    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        
        return String.format("%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
} 