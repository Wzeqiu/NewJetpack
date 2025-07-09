package com.wzeqiu.mediacode.editor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.common.kt.setTitleBarContent
import com.common.kt.viewBinding
import com.common.media.MediaInfo
import com.gyf.immersionbar.ktx.immersionBar
import com.wzeqiu.mediacode.R
import com.wzeqiu.mediacode.databinding.ActivityVideoEditorBinding
import com.wzeqiu.mediacode.editor.panel.AudioPanelFragment
import com.wzeqiu.mediacode.editor.panel.FilterPanelFragment
import com.wzeqiu.mediacode.editor.panel.StickerPanelFragment
import com.wzeqiu.mediacode.editor.panel.TextPanelFragment
import com.wzeqiu.mediacode.editor.timeline.VideoTimelineView
import android.net.Uri

/**
 * 视频编辑器Activity
 * 
 * 这是视频编辑工具的主界面，提供完整的视频编辑功能，包括：
 * - 视频剪辑（裁剪、分割、拼接、旋转、翻转、变速）
 * - 特效添加（滤镜、特效、动画、贴纸）
 * - 音频处理（提取、调节、混音、裁剪）
 * - 其他功能（字幕添加、格式转换）
 */
class VideoEditorActivity : AppCompatActivity() {

    private val viewBinding by viewBinding<ActivityVideoEditorBinding>()
    private lateinit var viewModel: VideoEditorViewModel
    
    // 当前编辑的媒体信息
    private var mediaInfo: MediaInfo? = null
    
    companion object {
        const val EXTRA_MEDIA_INFO = "extra_media_info"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[VideoEditorViewModel::class.java]
        
        // 获取传入的媒体信息
        mediaInfo = intent.getParcelableExtra(EXTRA_MEDIA_INFO)
        if (mediaInfo == null) {
            finish()
            return
        }
        
        // 设置沉浸式状态栏
        immersionBar { 
            titleBarMarginTop(viewBinding.titleBar.rlTitleBar)
            statusBarDarkFont(true)
        }
        
        // 设置标题栏
        setTitleBarContent(
            title = "视频编辑",
            rightText = "导出",
            rightAction = { exportMedia() }
        ) {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // 初始化UI
        initUI()
        
        // 初始化视频预览
        initVideoPreview()
        
        // 观察ViewModel中的数据变化
        observeViewModel()
        
        // 设置播放/暂停按钮点击事件
        viewBinding.btnPlayPause.setOnClickListener {
            viewModel.togglePlayPause()
        }
        
        // 设置进度条变化监听
        viewBinding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = viewModel.mediaDuration.value ?: 0
                    val position = duration * progress / 100
                    viewModel.seekTo(position)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })
    }
    
    /**
     * 初始化UI界面
     */
    private fun initUI() {
        viewBinding.apply {
            // 初始化底部功能按钮
            btnTrim.setOnClickListener { showTrimPanel() }
            btnFilter.setOnClickListener { showFilterPanel() }
            btnAudio.setOnClickListener { showAudioPanel() }
            btnText.setOnClickListener { showTextPanel() }
            btnSticker.setOnClickListener { showStickerPanel() }
            btnSpeed.setOnClickListener { showSpeedPanel() }
            btnMore.setOnClickListener { showMorePanel() }
        }
    }
    
    /**
     * 初始化视频预览
     */
    private fun initVideoPreview() {
        mediaInfo?.let { media ->
            // 加载媒体到ViewModel
            viewModel.loadMedia(media)
            
            // 设置PlayerView的播放器实例
            viewBinding.playerView.player = viewModel.getPlayer()
            
            // 初始化时间轴
            initTimeline(media)
            
            // 更新UI显示
            viewBinding.tvTotalTime.text = media.getFormattedDuration()
            viewBinding.tvCurrentTime.text = "00:00"
        }
    }
    
    /**
     * 初始化时间轴
     */
    private fun initTimeline(media: MediaInfo) {
        // 设置时间轴回调
        viewBinding.videoTimeline.timelineCallback = object : VideoTimelineView.TimelineCallback {
            override fun onTrimRangeChanged(startMs: Long, endMs: Long) {
                // 更新裁剪范围
                viewModel.setTrimRange(startMs, endMs)
            }
            
            override fun onPositionChanged(positionMs: Long) {
                // 更新播放位置
                viewModel.seekTo(positionMs)
            }
        }
        
        // 加载媒体到时间轴
        viewBinding.videoTimeline.loadMedia(Uri.parse(media.path), media.duration)
    }
    
    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察编辑进度
        viewModel.editorProgress.observe(this) { progress ->
            // 更新进度显示
        }
        
        // 观察导出状态
        viewModel.exportStatus.observe(this) { status ->
            // 处理导出状态变化
        }
        
        // 观察播放位置
        viewModel.playbackPosition.observe(this) { position ->
            // 更新文字覆盖层和贴纸覆盖层显示
            updateTextOverlayVisibility(position)
            updateStickerOverlayVisibility(position)
            
            // 更新时间轴位置
            viewBinding.videoTimeline.setCurrentPosition(position)
            
            // 更新进度条和时间显示
            updatePlaybackProgress(position)
        }
        
        // 观察文字覆盖层列表
        viewModel.textOverlayList.observe(this) { textOverlays ->
            // 文字覆盖层列表更新时，更新当前显示
            updateTextOverlayVisibility(viewModel.playbackPosition.value ?: 0)
        }
        
        // 观察贴纸覆盖层列表
        viewModel.stickerOverlayList.observe(this) { stickerOverlays ->
            // 贴纸覆盖层列表更新时，更新当前显示
            updateStickerOverlayVisibility(viewModel.playbackPosition.value ?: 0)
        }
        
        // 观察播放状态
        viewModel.isPlaying.observe(this) { isPlaying ->
            // 更新播放/暂停按钮图标
//            viewBinding.btnPlayPause.setImageResource(
//                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
//            )
        }
        
        // 观察媒体时长
        viewModel.mediaDuration.observe(this) { duration ->
            // 更新总时长显示
            val formattedDuration = formatDuration(duration)
            viewBinding.tvTotalTime.text = formattedDuration
        }
    }
    
    /**
     * 更新文字覆盖层可见性
     */
    private fun updateTextOverlayVisibility(position: Long) {
        // 获取当前时间点应该显示的文字覆盖层
        val visibleOverlays = viewModel.getVisibleTextOverlays(position)
        
        // 清除当前显示的文字
        viewBinding.textOverlayContainer.removeAllViews()
        
        // 添加应该显示的文字
        for (overlay in visibleOverlays) {
            val textView = android.widget.TextView(this).apply {
                text = overlay.text
                textSize = overlay.fontSize / resources.displayMetrics.density
                setTextColor(overlay.textColor)
                
                // 设置背景
                if (overlay.hasBackground) {
                    setBackgroundColor(overlay.backgroundColor)
                }
                
                // 设置位置
                x = viewBinding.playerView.width * overlay.xPosition - width / 2
                y = viewBinding.playerView.height * overlay.yPosition - height / 2
            }
            
            viewBinding.textOverlayContainer.addView(textView)
        }
    }
    
    /**
     * 显示裁剪面板
     */
    private fun showTrimPanel() {
        // 显示视频裁剪相关控件
        viewBinding.videoTimeline.showTrimControls(true)
        
        // 如果已经设置了裁剪范围，则恢复
        val startMs = viewModel.getTrimStartMs()
        val endMs = viewModel.getTrimEndMs()
        viewBinding.videoTimeline.setTrimRange(startMs, endMs)
    }
    
    /**
     * 显示滤镜面板
     */
    private fun showFilterPanel() {
        // 显示滤镜选择面板
        supportFragmentManager.beginTransaction()
            .replace(R.id.panel_container, FilterPanelFragment.newInstance())
            .commit()
    }
    
    /**
     * 显示音频处理面板
     */
    private fun showAudioPanel() {
        // 显示音频处理面板
        supportFragmentManager.beginTransaction()
            .replace(R.id.panel_container, AudioPanelFragment.newInstance())
            .commit()
    }
    
    /**
     * 显示文字/字幕面板
     */
    private fun showTextPanel() {
        // 显示文字编辑面板
        supportFragmentManager.beginTransaction()
            .replace(R.id.panel_container, TextPanelFragment.newInstance())
            .commit()
    }
    
    /**
     * 显示变速面板
     */
    private fun showSpeedPanel() {
        // 显示变速选项面板
    }
    
    /**
     * 显示更多功能面板
     */
    private fun showMorePanel() {
        // 显示更多编辑功能
    }
    
    /**
     * 显示贴纸面板
     */
    private fun showStickerPanel() {
        // 显示贴纸编辑面板
        supportFragmentManager.beginTransaction()
            .replace(R.id.panel_container, StickerPanelFragment.newInstance())
            .commit()
    }
    
    /**
     * 更新贴纸覆盖层可见性
     */
    private fun updateStickerOverlayVisibility(position: Long) {
        // 获取当前时间点应该显示的贴纸覆盖层
        val visibleOverlays = viewModel.getVisibleStickerOverlays(position)
        
        // 清除当前显示的贴纸
        viewBinding.stickerOverlayContainer.removeAllViews()
        
        // 添加应该显示的贴纸
        for (overlay in visibleOverlays) {
            // 创建图像视图
            val imageView = android.widget.ImageView(this).apply {
                // 设置图像资源
                setImageResource(overlay.resourceId)
                
                // 计算贴纸尺寸（以播放器宽度为基准）
                val width = (viewBinding.playerView.width * overlay.size).toInt()
                val height = width
                
                // 设置布局参数
                layoutParams = android.view.ViewGroup.LayoutParams(width, height)
                
                // 设置旋转
                rotation = overlay.rotation
                
                // 设置透明度
                alpha = overlay.alpha / 255f
                
                // 设置位置
                x = viewBinding.playerView.width * overlay.xPosition - width / 2
                y = viewBinding.playerView.height * overlay.yPosition - height / 2
            }
            
            // 添加到容器
            viewBinding.stickerOverlayContainer.addView(imageView)
        }
    }
    
    /**
     * 导出媒体文件
     */
    private fun exportMedia() {
        // 执行导出操作
        viewModel.exportMedia(this)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 释放资源前先解除PlayerView与Player的绑定
        viewBinding.playerView.player = null
        // 释放ViewModel中的资源
        viewModel.releaseResources()
    }
    
    /**
     * 更新播放进度
     * 
     * @param position 当前播放位置（毫秒）
     */
    private fun updatePlaybackProgress(position: Long) {
        // 更新当前时间显示
        viewBinding.tvCurrentTime.text = formatDuration(position)
        
        // 更新进度条
        val duration = viewModel.mediaDuration.value ?: 1L
        val progress = ((position.toFloat() / duration) * 100).toInt()
        viewBinding.seekBar.progress = progress
    }
    
    /**
     * 格式化时长
     * 
     * @param durationMs 时长（毫秒）
     * @return 格式化后的时长字符串
     */
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
} 