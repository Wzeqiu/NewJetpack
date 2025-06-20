package com.common.widget.video

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.common.common.R

/**
 * 自定义视频播放器视图
 * 集成了圆角、缓存、播放控制等功能
 */
@UnstableApi
class CustomVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val playerView: RoundedExoPlayerView
    private val progressBar: View
    private val errorView: View
    private val playPauseButton: View
    private val retryButton: View
    
    private val videoPlayerManager: VideoPlayerManager = VideoPlayerManager.getInstance(context)
    private var videoPlayerListener: VideoPlayerListener? = null
    
    init {
        // 加载布局
        val view = inflate(context, R.layout.view_custom_video_player, this)
        
        // 初始化视图
        playerView = view.findViewById(R.id.player_view)
        progressBar = view.findViewById(R.id.progress_bar)
        errorView = view.findViewById(R.id.error_view)
        playPauseButton = view.findViewById(R.id.play_pause_button)
        retryButton = view.findViewById(R.id.retry_button)
        
        // 设置播放器监听器
        videoPlayerManager.setVideoPlayerListener(object : VideoPlayerManager.VideoPlayerListener {
            override fun onPlayerStateChanged(state: VideoPlayerManager.VideoPlayerState) {
                when (state) {
                    VideoPlayerManager.VideoPlayerState.BUFFERING -> {
                        progressBar.isVisible = true
                        videoPlayerListener?.onBuffering()
                    }
                    VideoPlayerManager.VideoPlayerState.READY -> {
                        progressBar.isVisible = false
                        videoPlayerListener?.onReady()
                    }
                    VideoPlayerManager.VideoPlayerState.ENDED -> {
                        videoPlayerListener?.onCompletion()
                    }
                    else -> {
                        progressBar.isVisible = false
                    }
                }
            }
            
            override fun onPlayingStateChanged(isPlaying: Boolean) {
                updatePlayPauseButton(isPlaying)
                videoPlayerListener?.onPlayingStateChanged(isPlaying)
            }
            
            override fun onVideoDurationReady(durationMs: Long) {
                videoPlayerListener?.onVideoDurationReady(durationMs)
            }
            
            override fun onPlayerError(error: PlaybackException) {
                progressBar.isVisible = false
                errorView.isVisible = true
                videoPlayerListener?.onError(error)
            }
        })
        
        // 绑定播放器视图
        videoPlayerManager.attachPlayerView(playerView)
        
        // 设置控制按钮点击事件
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }
        
        retryButton.setOnClickListener {
            errorView.isVisible = false
            videoPlayerManager.prepareVideo(videoPlayerManager.getCurrentVideoUri(), true)
        }
        
        // 从属性中获取配置
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomVideoPlayerView)
        try {
            // 设置圆角
            val cornerRadius = typedArray.getDimension(R.styleable.CustomVideoPlayerView_cornerRadius, 0f)
            setCornerRadius(cornerRadius)
            
            // 设置调整大小模式
            val resizeMode = typedArray.getInt(R.styleable.CustomVideoPlayerView_resizeMode, AspectRatioFrameLayout.RESIZE_MODE_FIT)
            setResizeMode(resizeMode)
            
            // 设置是否显示控制器
            val showController = typedArray.getBoolean(R.styleable.CustomVideoPlayerView_showController, true)
            setShowController(showController)
            
            // 设置自动播放
            val autoPlay = typedArray.getBoolean(R.styleable.CustomVideoPlayerView_autoPlay, false)
            setAutoPlay(autoPlay)
        } finally {
            typedArray.recycle()
        }
    }
    
    /**
     * 设置视频播放监听器
     */
    fun setVideoPlayerListener(listener: VideoPlayerListener) {
        this.videoPlayerListener = listener
    }
    
    /**
     * 设置视频源
     * @param videoPath 视频路径（本地文件路径或URL）
     * @param autoPlay 是否自动播放
     */
    fun setVideoPath(videoPath: String, autoPlay: Boolean = true) {
        errorView.isVisible = false
        videoPlayerManager.prepareVideo(videoPath, autoPlay)
    }
    
    /**
     * 设置视频源
     * @param videoUri 视频URI
     * @param autoPlay 是否自动播放
     */
    fun setVideoUri(videoUri: Uri, autoPlay: Boolean = true) {
        errorView.isVisible = false
        videoPlayerManager.prepareVideo(videoUri, autoPlay)
    }
    
    /**
     * 开始播放
     */
    fun play() {
        videoPlayerManager.play()
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        videoPlayerManager.pause()
    }
    
    /**
     * 切换播放/暂停状态
     */
    fun togglePlayPause() {
        if (videoPlayerManager.isPlaying()) {
            pause()
        } else {
            play()
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        videoPlayerManager.stop()
    }
    
    /**
     * 释放资源
     * 在Activity或Fragment的onDestroy中调用
     */
    fun release() {
        videoPlayerManager.release()
    }
    
    /**
     * 跳转到指定位置
     * @param positionMs 毫秒
     */
    fun seekTo(positionMs: Long) {
        videoPlayerManager.seekTo(positionMs)
    }
    
    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return videoPlayerManager.getCurrentPosition()
    }
    
    /**
     * 获取视频总时长
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long {
        return videoPlayerManager.getDuration()
    }
    
    /**
     * 获取当前播放状态
     * @return 是否正在播放
     */
    fun isPlaying(): Boolean {
        return videoPlayerManager.isPlaying()
    }
    
    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    fun setPlaybackSpeed(speed: Float) {
        videoPlayerManager.setPlaybackSpeed(speed)
    }
    
    /**
     * 设置音量
     * @param volume 音量（0.0 - 1.0）
     */
    fun setVolume(volume: Float) {
        videoPlayerManager.setVolume(volume)
    }
    
    /**
     * 设置圆角半径
     * @param radius 圆角半径（像素）
     */
    fun setCornerRadius(radius: Float) {
        playerView.cornerRadius = radius
    }
    
    /**
     * 设置四个角的圆角半径
     */
    fun setCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        playerView.setCornerRadii(topLeft, topRight, bottomLeft, bottomRight)
    }
    
    /**
     * 设置调整大小模式
     * @param resizeMode 调整大小模式
     * AspectRatioFrameLayout.RESIZE_MODE_FIT - 适应（保持宽高比）
     * AspectRatioFrameLayout.RESIZE_MODE_FILL - 填充（可能裁剪）
     * AspectRatioFrameLayout.RESIZE_MODE_ZOOM - 缩放（保持宽高比，可能裁剪）
     * AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH - 固定宽度
     * AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT - 固定高度
     */
    fun setResizeMode(@AspectRatioFrameLayout.ResizeMode resizeMode: Int) {
        playerView.resizeMode = resizeMode
    }
    
    /**
     * 设置是否显示控制器
     * @param show 是否显示
     */
    fun setShowController(show: Boolean) {
        playerView.useController = show
    }
    
    /**
     * 设置是否自动播放
     * @param autoPlay 是否自动播放
     */
    fun setAutoPlay(autoPlay: Boolean) {
        playerView.player?.playWhenReady = autoPlay
    }
    
    /**
     * 更新播放/暂停按钮状态
     */
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        // 根据播放状态更新按钮图标
        if (playPauseButton is ImageButton) {
            (playPauseButton as ImageButton).setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }
    
    /**
     * 视频播放监听器接口
     */
    interface VideoPlayerListener {
        /**
         * 视频准备就绪
         */
        fun onReady() {}
        
        /**
         * 视频缓冲中
         */
        fun onBuffering() {}
        
        /**
         * 播放状态变化
         */
        fun onPlayingStateChanged(isPlaying: Boolean) {}
        
        /**
         * 视频播放完成
         */
        fun onCompletion() {}
        
        /**
         * 视频时长准备完成
         */
        fun onVideoDurationReady(durationMs: Long) {}
        
        /**
         * 播放错误
         */
        fun onError(error: PlaybackException) {}
    }
} 