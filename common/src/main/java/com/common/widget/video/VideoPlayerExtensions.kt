package com.common.widget.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

/**
 * 视频播放器扩展函数
 */

/**
 * 使用VideoPlayerActivity播放视频
 * @param videoUrl 视频URL
 * @param videoTitle 视频标题
 */
fun Context.playVideo(videoUrl: String, videoTitle: String? = null) {
    val intent = Intent(this, VideoPlayerActivity::class.java).apply {
        putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl)
        putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, videoTitle)
    }
    startActivity(intent)
}

/**
 * 使用VideoPlayerActivity播放视频
 * @param videoUri 视频URI
 * @param videoTitle 视频标题
 */
fun Context.playVideo(videoUri: Uri, videoTitle: String? = null) {
    playVideo(videoUri.toString(), videoTitle)
}

/**
 * Fragment扩展函数，使用VideoPlayerActivity播放视频
 * @param videoUrl 视频URL
 * @param videoTitle 视频标题
 */
fun Fragment.playVideo(videoUrl: String, videoTitle: String? = null) {
    context?.playVideo(videoUrl, videoTitle)
}

/**
 * Fragment扩展函数，使用VideoPlayerActivity播放视频
 * @param videoUri 视频URI
 * @param videoTitle 视频标题
 */
fun Fragment.playVideo(videoUri: Uri, videoTitle: String? = null) {
    context?.playVideo(videoUri, videoTitle)
}

/**
 * Activity扩展函数，在当前Activity中添加视频播放器
 * @param videoUrl 视频URL
 * @param videoPlayerView 视频播放器视图
 * @param autoPlay 是否自动播放
 * @param listener 播放监听器
 */
fun Activity.setupVideoPlayer(
    videoUrl: String,
    videoPlayerView: CustomVideoPlayerView,
    autoPlay: Boolean = true,
    listener: CustomVideoPlayerView.VideoPlayerListener? = null
) {
    VideoPlayerHelper.playNetworkVideo(videoPlayerView, videoUrl, autoPlay, listener)
}

/**
 * Fragment扩展函数，在当前Fragment中添加视频播放器
 * @param videoUrl 视频URL
 * @param videoPlayerView 视频播放器视图
 * @param autoPlay 是否自动播放
 * @param listener 播放监听器
 */
fun Fragment.setupVideoPlayer(
    videoUrl: String,
    videoPlayerView: CustomVideoPlayerView,
    autoPlay: Boolean = true,
    listener: CustomVideoPlayerView.VideoPlayerListener? = null
) {
    activity?.let {
        VideoPlayerHelper.playNetworkVideo(videoPlayerView, videoUrl, autoPlay, listener)
    }
} 