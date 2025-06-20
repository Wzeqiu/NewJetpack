package com.common.widget.video

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.common.common.R
import com.common.ui.BaseActivity
import com.common.common.databinding.ActivityVideoPlayerBinding
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

/**
 * 视频播放器示例Activity
 */
class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>() {
    
    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
    }
    
    private var videoUrl: String? = null
    private var videoTitle: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 获取传入的视频URL和标题
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        
        // 设置标题
        binding.titleTextView.text = videoTitle ?: "视频播放"
        
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 设置圆角
        binding.videoPlayerView.setCornerRadius(16f)
        
        // 设置调整大小模式
        binding.videoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
        
        // 设置播放监听器
        binding.videoPlayerView.setVideoPlayerListener(object : CustomVideoPlayerView.VideoPlayerListener {
            override fun onReady() {
                // 视频准备就绪
                binding.progressBar.hide()
            }
            
            override fun onBuffering() {
                // 视频缓冲中
                binding.progressBar.show()
            }
            
            override fun onPlayingStateChanged(isPlaying: Boolean) {
                // 播放状态变化
                binding.playPauseButton.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                )
            }
            
            override fun onCompletion() {
                // 视频播放完成
                Toast.makeText(this@VideoPlayerActivity, "播放完成", Toast.LENGTH_SHORT).show()
            }
            
            override fun onVideoDurationReady(durationMs: Long) {
                // 视频时长准备完成
                binding.durationTextView.text = VideoPlayerHelper.formatDuration(durationMs)
            }
            
            override fun onError(error: PlaybackException) {
                // 播放错误
                binding.progressBar.hide()
                Toast.makeText(this@VideoPlayerActivity, "播放错误: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        
        // 播放/暂停按钮点击事件
        binding.playPauseButton.setOnClickListener {
            binding.videoPlayerView.togglePlayPause()
        }
        
        // 全屏按钮点击事件
        binding.fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }
        
        // 播放视频
        videoUrl?.let {
            binding.progressBar.show()
            binding.videoPlayerView.setVideoPath(it, true)
        }
    }
    
    /**
     * 切换全屏模式
     */
    private fun toggleFullscreen() {
        // 在实际应用中，这里可以实现全屏切换逻辑
        Toast.makeText(this, "全屏功能待实现", Toast.LENGTH_SHORT).show()
    }
    
    override fun onPause() {
        super.onPause()
        // 暂停播放
        binding.videoPlayerView.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放资源
        binding.videoPlayerView.release()
    }
} 