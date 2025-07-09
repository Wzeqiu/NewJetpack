package com.wzeqiu.mediacode.editor.timeline

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wzeqiu.mediacode.R
import com.wzeqiu.mediacode.databinding.ViewVideoTimelineBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 视频时间轴视图
 * 
 * 提供视频缩略图、时间指示器和编辑标记功能，支持：
 * - 显示视频缩略图序列
 * - 显示时间刻度
 * - 视频裁剪范围选择
 * - 编辑标记显示（文字、贴纸等）
 */
class VideoTimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 视图绑定
    private val binding = ViewVideoTimelineBinding.inflate(LayoutInflater.from(context), this, true)
    
    // 缩略图适配器
    private val thumbnailAdapter = ThumbnailAdapter()
    
    // 时间指示器适配器
    private val timeIndicatorAdapter = TimeIndicatorAdapter()
    
    // 协程作用域
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    
    // 缩略图宽度
    private val thumbnailWidth = resources.getDimensionPixelSize(R.dimen.thumbnail_width)
    
    // 视频总时长（毫秒）
    private var mediaDurationMs: Long = 0
    
    // 裁剪开始时间（毫秒）
    private var trimStartMs: Long = 0
    
    // 裁剪结束时间（毫秒）
    private var trimEndMs: Long = 0
    
    // 当前播放位置（毫秒）
    private var currentPositionMs: Long = 0
    
    // 滚动状态
    private var isScrolling = false
    
    // 时间轴回调接口
    var timelineCallback: TimelineCallback? = null
    
    // 裁剪开始手柄X坐标
    private var trimStartX = 0f
    
    // 裁剪结束手柄X坐标
    private var trimEndX = 0f
    
    // 裁剪手柄拖动状态
    private var isDraggingStart = false
    private var isDraggingEnd = false
    
    init {
        // 初始化视图
        initViews()
        
        // 初始化拖拽监听
        initDragListeners()
    }
    
    /**
     * 初始化视图
     */
    private fun initViews() {
        // 设置缩略图列表
        binding.recyclerThumbnails.apply {
            adapter = thumbnailAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        
        // 设置时间指示器列表
        binding.recyclerTimeIndicators.apply {
            adapter = timeIndicatorAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        
        // 默认隐藏裁剪控制器
        binding.trimHandlersContainer.visibility = View.GONE
    }
    
    /**
     * 初始化拖拽监听
     */
    private fun initDragListeners() {
        // 裁剪开始手柄拖拽
        binding.trimStartHandler.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDraggingStart = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingStart) {
                        val newX = event.rawX
                        moveTrimStartHandler(newX)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingStart) {
                        isDraggingStart = false
                        updateTrimRange()
                        true
                    } else false
                }
                else -> false
            }
        }
        
        // 裁剪结束手柄拖拽
        binding.trimEndHandler.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDraggingEnd = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingEnd) {
                        val newX = event.rawX
                        moveTrimEndHandler(newX)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingEnd) {
                        isDraggingEnd = false
                        updateTrimRange()
                        true
                    } else false
                }
                else -> false
            }
        }
    }
    
    /**
     * 移动裁剪开始手柄
     */
    private fun moveTrimStartHandler(newX: Float) {
        // 获取RecyclerView在屏幕中的坐标
        val recyclerLocation = IntArray(2)
        binding.recyclerThumbnails.getLocationOnScreen(recyclerLocation)
        
        // 计算相对于RecyclerView的X坐标
        val relativeX = newX - recyclerLocation[0]
        
        // 限制范围（不能超过结束手柄）
        val maxX = trimEndX - binding.trimEndHandler.width
        val limitedX = relativeX.coerceIn(0f, maxX)
        
        // 更新手柄位置
        binding.trimStartHandler.x = limitedX
        
        // 更新指示器
        updateTrimIndicators()
    }
    
    /**
     * 移动裁剪结束手柄
     */
    private fun moveTrimEndHandler(newX: Float) {
        // 获取RecyclerView在屏幕中的坐标
        val recyclerLocation = IntArray(2)
        binding.recyclerThumbnails.getLocationOnScreen(recyclerLocation)
        
        // 计算相对于RecyclerView的X坐标
        val relativeX = newX - recyclerLocation[0]
        
        // 限制范围（不能小于开始手柄）
        val minX = trimStartX + binding.trimStartHandler.width
        val maxX = binding.recyclerThumbnails.width.toFloat()
        val limitedX = relativeX.coerceIn(minX, maxX)
        
        // 更新手柄位置
        binding.trimEndHandler.x = limitedX
        
        // 更新指示器
        updateTrimIndicators()
    }
    
    /**
     * 更新裁剪指示器
     */
    private fun updateTrimIndicators() {
        // 获取手柄位置
        val startX = binding.trimStartHandler.x
        val endX = binding.trimEndHandler.x
        
        // 设置顶部指示器位置和宽度
        binding.trimRangeIndicator.apply {
            x = startX
            layoutParams.width = (endX - startX).toInt()
            requestLayout()
        }
        
        // 设置底部指示器位置和宽度
        binding.trimRangeIndicatorBottom.apply {
            x = startX
            layoutParams.width = (endX - startX).toInt()
            requestLayout()
        }
    }
    
    /**
     * 更新裁剪范围
     */
    private fun updateTrimRange() {
        // 计算裁剪范围的时间值
        val startX = binding.trimStartHandler.x
        val endX = binding.trimEndHandler.x
        
        // 时间轴总宽度
        val totalWidth = binding.recyclerThumbnails.width
        
        // 计算对应的时间
        trimStartMs = (startX / totalWidth * mediaDurationMs).toLong()
        trimEndMs = (endX / totalWidth * mediaDurationMs).toLong()
        
        // 通知回调
        timelineCallback?.onTrimRangeChanged(trimStartMs, trimEndMs)
    }
    
    /**
     * 加载媒体
     */
    fun loadMedia(videoUri: Uri, durationMs: Long) {
        mediaDurationMs = durationMs
        trimStartMs = 0
        trimEndMs = durationMs
        
        // 加载缩略图
        loadThumbnails(videoUri, durationMs)
        
        // 加载时间指示器
        loadTimeIndicators(durationMs)
    }
    
    /**
     * 加载缩略图
     */
    private fun loadThumbnails(videoUri: Uri, durationMs: Long) {
        coroutineScope.launch {
            // 计算需要提取的缩略图数量
            val thumbnailCount = calculateThumbnailCount(durationMs)
            
            // 提取缩略图
            val thumbnails = withContext(Dispatchers.IO) {
                extractThumbnails(videoUri, durationMs, thumbnailCount)
            }
            
            // 更新适配器
            thumbnailAdapter.submitList(thumbnails)
        }
    }
    
    /**
     * 计算需要提取的缩略图数量
     */
    private fun calculateThumbnailCount(durationMs: Long): Int {
        // 根据视频时长计算合适的缩略图数量
        return (durationMs / 1000).toInt().coerceAtLeast(10)
    }
    
    /**
     * 提取缩略图
     */
    private suspend fun extractThumbnails(
        videoUri: Uri,
        durationMs: Long,
        count: Int
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val thumbnails = mutableListOf<Bitmap>()
        
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            // 计算时间间隔
            val interval = durationMs / count
            
            // 提取缩略图
            for (i in 0 until count) {
                val timeUs = (i * interval) * 1000 // 微秒
                val bitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                
                bitmap?.let {
                    thumbnails.add(it)
                }
            }
            
            retriever.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        thumbnails
    }
    
    /**
     * 加载时间指示器
     */
    private fun loadTimeIndicators(durationMs: Long) {
        // 计算时间指示器数量
        val count = calculateThumbnailCount(durationMs)
        
        // 创建时间指示器数据
        val timeIndicators = List(count) { index ->
            val timeMs = index * (durationMs / count)
            formatTime(timeMs)
        }
        
        // 更新适配器
        timeIndicatorAdapter.submitList(timeIndicators)
    }
    
    /**
     * 格式化时间
     */
    private fun formatTime(timeMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * 设置当前播放位置
     */
    fun setCurrentPosition(positionMs: Long) {
        currentPositionMs = positionMs
        
        // 计算位置占比
        val positionRatio = positionMs.toFloat() / mediaDurationMs
        
        // 计算对应的X坐标
        val scrollX = (binding.recyclerThumbnails.width * positionRatio).toInt()
        
        // 滚动到指定位置
        if (!isScrolling) {
            binding.recyclerThumbnails.scrollTo(scrollX, 0)
            binding.recyclerTimeIndicators.scrollTo(scrollX, 0)
        }
    }
    
    /**
     * 显示裁剪控制器
     */
    fun showTrimControls(show: Boolean) {
        binding.trimHandlersContainer.visibility = if (show) View.VISIBLE else View.GONE
        
        if (show) {
            // 初始化裁剪控制器位置
            initTrimControlsPosition()
        }
    }
    
    /**
     * 初始化裁剪控制器位置
     */
    private fun initTrimControlsPosition() {
        // 设置开始手柄位置
        binding.trimStartHandler.x = 0f
        trimStartX = 0f
        
        // 设置结束手柄位置
        binding.trimEndHandler.x = binding.recyclerThumbnails.width.toFloat()
        trimEndX = binding.recyclerThumbnails.width.toFloat()
        
        // 更新指示器
        updateTrimIndicators()
    }
    
    /**
     * 设置裁剪范围
     */
    fun setTrimRange(startMs: Long, endMs: Long) {
        // 更新时间值
        trimStartMs = startMs
        trimEndMs = endMs
        
        // 计算位置比例
        val startRatio = startMs.toFloat() / mediaDurationMs
        val endRatio = endMs.toFloat() / mediaDurationMs
        
        // 计算X坐标
        val startX = binding.recyclerThumbnails.width * startRatio
        val endX = binding.recyclerThumbnails.width * endRatio
        
        // 更新手柄位置
        binding.trimStartHandler.x = startX
        binding.trimEndHandler.x = endX
        trimStartX = startX
        trimEndX = endX
        
        // 更新指示器
        updateTrimIndicators()
    }
    
    /**
     * 缩略图适配器
     */
    inner class ThumbnailAdapter :
        RecyclerView.Adapter<ThumbnailAdapter.ThumbnailViewHolder>() {
        
        private var thumbnails: List<Bitmap> = emptyList()
        
        fun submitList(list: List<Bitmap>) {
            thumbnails = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_thumbnail, parent, false)
            return ThumbnailViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
            holder.bind(thumbnails[position])
        }
        
        override fun getItemCount(): Int = thumbnails.size
        
        inner class ThumbnailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageView: ImageView = itemView as ImageView
            
            fun bind(bitmap: Bitmap) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }
    
    /**
     * 时间指示器适配器
     */
    inner class TimeIndicatorAdapter :
        RecyclerView.Adapter<TimeIndicatorAdapter.TimeIndicatorViewHolder>() {
        
        private var timeIndicators: List<String> = emptyList()
        
        fun submitList(list: List<String>) {
            timeIndicators = list
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeIndicatorViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_time_indicator, parent, false)
            return TimeIndicatorViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: TimeIndicatorViewHolder, position: Int) {
            holder.bind(timeIndicators[position])
        }
        
        override fun getItemCount(): Int = timeIndicators.size
        
        inner class TimeIndicatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textView: TextView = itemView.findViewById(R.id.tv_time)
            
            fun bind(time: String) {
                textView.text = time
            }
        }
    }
    
    /**
     * 时间轴回调接口
     */
    interface TimelineCallback {
        /**
         * 裁剪范围变化回调
         */
        fun onTrimRangeChanged(startMs: Long, endMs: Long)
        
        /**
         * 位置变化回调
         */
        fun onPositionChanged(positionMs: Long)
    }
} 