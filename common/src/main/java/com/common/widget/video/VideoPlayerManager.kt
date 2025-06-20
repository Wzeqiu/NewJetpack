package com.common.widget.video

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.File

/**
 * 视频播放管理器
 * 封装Media3，提供简单易用的视频播放API
 */
@UnstableApi
class VideoPlayerManager private constructor(private val context: Context) {
    
    private var exoPlayer: ExoPlayer? = null
    private var videoListener: VideoPlayerListener? = null
    private var currentVideoUri: Uri? = null
    private var isPlaying = false
    
    companion object {
        @Volatile
        private var instance: VideoPlayerManager? = null
        
        fun getInstance(context: Context): VideoPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: VideoPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * 初始化播放器
     */
    private fun initializePlayer() {
        if (exoPlayer == null) {
            val cacheDataSourceFactory = VideoCacheManager.buildCacheDataSourceFactory(context)
            val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
            
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
            
            exoPlayer?.addListener(playerListener)
        }
    }
    
    /**
     * 设置播放器监听器
     */
    fun setVideoPlayerListener(listener: VideoPlayerListener) {
        this.videoListener = listener
    }
    
    /**
     * 准备播放视频
     * @param videoUri 视频URI
     * @param autoPlay 是否自动播放
     */
    fun prepareVideo(videoUri: Uri, autoPlay: Boolean = true) {
        initializePlayer()
        currentVideoUri = videoUri
        
        val mediaSource = buildMediaSource(videoUri)
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = autoPlay
            isPlaying = autoPlay
        }
    }
    
    /**
     * 准备播放视频
     * @param videoPath 视频路径
     * @param autoPlay 是否自动播放
     */
    fun prepareVideo(videoPath: String, autoPlay: Boolean = true) {
        val uri = if (videoPath.startsWith("http")) {
            Uri.parse(videoPath)
        } else {
            Uri.fromFile(File(videoPath))
        }
        prepareVideo(uri, autoPlay)
    }
    
    /**
     * 根据URI构建适合的媒体源
     */
    private fun buildMediaSource(uri: Uri): MediaSource {
        val cacheDataSourceFactory = VideoCacheManager.buildCacheDataSourceFactory(context)
        val mediaItem = MediaItem.fromUri(uri)
        
        return when {
            uri.toString().contains(".m3u8") -> {
                HlsMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            uri.toString().contains(".mpd") -> {
                DashMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            uri.toString().startsWith("rtsp://") -> {
                RtspMediaSource.Factory()
                    .createMediaSource(mediaItem)
            }
            else -> {
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }
    
    /**
     * 获取当前视频URI
     * @return 当前视频URI
     */
    fun getCurrentVideoUri(): Uri? {
        return currentVideoUri
    }
    
    /**
     * 开始播放
     */
    fun play() {
        exoPlayer?.apply {
            playWhenReady = true
            isPlaying = true
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        exoPlayer?.apply {
            playWhenReady = false
            isPlaying = false
        }
    }
    
    /**
     * 停止播放
     */
    fun stop() {
        exoPlayer?.apply {
            stop()
            isPlaying = false
        }
    }
    
    /**
     * 释放播放器资源
     */
    fun release() {
        exoPlayer?.apply {
            removeListener(playerListener)
            release()
        }
        exoPlayer = null
        isPlaying = false
    }
    
    /**
     * 跳转到指定位置
     * @param positionMs 毫秒
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }
    
    /**
     * 获取视频总时长
     * @return 总时长（毫秒）
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }
    
    /**
     * 获取当前播放状态
     * @return 是否正在播放
     */
    fun isPlaying(): Boolean {
        return isPlaying
    }
    
    /**
     * 设置播放速度
     * @param speed 播放速度
     */
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackSpeed(speed)
    }
    
    /**
     * 设置音量
     * @param volume 音量（0.0 - 1.0）
     */
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume
    }
    
    /**
     * 绑定播放器视图
     * @param playerView 播放器视图
     */
    fun attachPlayerView(playerView: RoundedExoPlayerView) {
        initializePlayer()
        playerView.player = exoPlayer
    }
    
    /**
     * 播放器事件监听器
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    videoListener?.onPlayerStateChanged(VideoPlayerState.IDLE)
                }
                Player.STATE_BUFFERING -> {
                    videoListener?.onPlayerStateChanged(VideoPlayerState.BUFFERING)
                }
                Player.STATE_READY -> {
                    videoListener?.onPlayerStateChanged(VideoPlayerState.READY)
                    videoListener?.onVideoDurationReady(exoPlayer?.duration ?: 0)
                }
                Player.STATE_ENDED -> {
                    videoListener?.onPlayerStateChanged(VideoPlayerState.ENDED)
                    isPlaying = false
                }
            }
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            this@VideoPlayerManager.isPlaying = isPlaying
            videoListener?.onPlayingStateChanged(isPlaying)
        }
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            videoListener?.onPlayerError(error)
        }
    }
    
    /**
     * 视频播放状态
     */
    enum class VideoPlayerState {
        IDLE, BUFFERING, READY, ENDED
    }
    
    /**
     * 视频播放监听器接口
     */
    interface VideoPlayerListener {
        /**
         * 播放器状态变化
         */
        fun onPlayerStateChanged(state: VideoPlayerState)
        
        /**
         * 播放状态变化
         */
        fun onPlayingStateChanged(isPlaying: Boolean)
        
        /**
         * 视频时长准备完成
         */
        fun onVideoDurationReady(durationMs: Long)
        
        /**
         * 播放错误
         */
        fun onPlayerError(error: androidx.media3.common.PlaybackException)
    }
} 