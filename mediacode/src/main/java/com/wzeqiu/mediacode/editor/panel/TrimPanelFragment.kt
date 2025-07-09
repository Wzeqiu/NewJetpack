package com.wzeqiu.mediacode.editor.panel

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.TimeUtils
import com.chad.library.adapter4.BaseQuickAdapter
import com.common.kt.adapter.ViewBindingHolder
import com.common.kt.activity.launch
import com.common.kt.load
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.wzeqiu.mediacode.databinding.ItemVideoBinding
import com.wzeqiu.mediacode.databinding.PanelTrimBinding
import com.wzeqiu.mediacode.editor.VideoEditorViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 视频裁剪面板Fragment
 * 
 * 提供视频裁剪、分割、拼接等功能
 */
class TrimPanelFragment : Fragment() {
    
    private lateinit var binding: PanelTrimBinding
    private lateinit var viewModel: VideoEditorViewModel
    
    // 分割点列表适配器
    private val splitPointsAdapter = SplitPointAdapter()
    
    // 拼接视频列表适配器
    private val videosAdapter = VideoAdapter()
    
    // 添加的视频列表
    private val additionalVideos = mutableListOf<MediaInfo>()
    
    // 分割点列表（毫秒）
    private val splitPoints = mutableListOf<Long>()
    
    companion object {
        fun newInstance(): TrimPanelFragment {
            return TrimPanelFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PanelTrimBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 获取ViewModel
        viewModel = ViewModelProvider(requireActivity())[VideoEditorViewModel::class.java]
        
        // 初始化UI
        initUI()
        
        // 初始化监听器
        initListeners()
        
        // 设置裁剪时间范围
        updateTrimRange()
        
        // 观察ViewModel中的数据变化
        observeViewModel()
    }
    
    /**
     * 初始化UI
     */
    private fun initUI() {
        // 设置分割点列表
        binding.rvSplitPoints.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = splitPointsAdapter
        }
        
        // 设置视频列表
        binding.rvVideos.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = videosAdapter
        }
        
        // 设置分割点适配器
        splitPointsAdapter.setOnItemClickListener { adapter, _, position ->
            // 点击移除分割点
            splitPoints.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        
        // 设置视频适配器
        videosAdapter.setOnItemClickListener { adapter, _, position ->
            // 点击移除视频
            additionalVideos.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
    }
    
    /**
     * 初始化监听器
     */
    private fun initListeners() {
        binding.apply {
            // 添加分割点
            btnAddSplit.setOnClickListener {
                val timeStr = etSplitPoint.text.toString()
                val timeMs = parseTimeString(timeStr)
                
                if (timeMs > 0) {
                    // 检查分割点是否在有效范围内
                    val duration = viewModel.mediaDuration.value ?: 0L
                    if (timeMs >= duration) {
                        Toast.makeText(requireContext(), "分割点不能超过视频时长", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    
                    // 添加分割点
                    splitPoints.add(timeMs)
                    splitPoints.sort()
                    splitPointsAdapter.submitList(splitPoints.toList())
                    etSplitPoint.text.clear()
                } else {
                    Toast.makeText(requireContext(), "请输入有效的时间格式(HH:MM:SS)", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 添加其他视频
            btnAddVideo.setOnClickListener {
                requireActivity().launch(
                    MediaManageActivity.getIntent(
                        requireActivity() as AppCompatActivity,
                        MediaConfig(
                            MediaConfig.MEDIA_TYPE_VIDEO,
                            originalMedia = true,
                            enableMultiSelect = true,
                            maxSelectCount = 5
                        )
                    )
                ) { result ->
                    if (result.resultCode == android.app.Activity.RESULT_OK) {
                        result.data?.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA)?.let { mediaList ->
                            // 添加到视频列表
                            additionalVideos.addAll(mediaList)
                            videosAdapter.submitList(additionalVideos.toList())
                        }
                    }
                }
            }
            
            // 预览裁剪
            btnPreviewTrim.setOnClickListener {
                val startTimeStr = etStartTime.text.toString()
                val endTimeStr = etEndTime.text.toString()
                
                val startTimeMs = parseTimeString(startTimeStr)
                val endTimeMs = parseTimeString(endTimeStr)
                
                if (startTimeMs >= 0 && endTimeMs > 0 && endTimeMs > startTimeMs) {
                    // 设置裁剪范围
                    viewModel.setTrimRange(startTimeMs, endTimeMs)
                    
                    // 更新显示
                    updateTrimRange()
                    
                    // 移动播放器到裁剪开始位置
                    viewModel.seekTo(startTimeMs)
                } else {
                    Toast.makeText(requireContext(), "请输入有效的时间范围", Toast.LENGTH_SHORT).show()
                }
            }
            
            // 重置裁剪
            btnResetTrim.setOnClickListener {
                val duration = viewModel.mediaDuration.value ?: 0L
                
                // 重置裁剪范围
                viewModel.setTrimRange(0, duration)
                
                // 清空输入框
                etStartTime.text.clear()
                etEndTime.text.clear()
                
                // 更新显示
                updateTrimRange()
            }
            
            // 应用更改
            btnApplyTrim.setOnClickListener {
                // 如果有分割点，执行分割
                if (splitPoints.isNotEmpty()) {
                    // 执行分割操作
                    // 这里只是示例，实际应用中需要实现分割逻辑
                    Toast.makeText(requireContext(), "分割功能尚未实现", Toast.LENGTH_SHORT).show()
                } else if (additionalVideos.isNotEmpty()) {
                    // 执行拼接操作
                    // 这里只是示例，实际应用中需要实现拼接逻辑
                    Toast.makeText(requireContext(), "拼接功能尚未实现", Toast.LENGTH_SHORT).show()
                } else {
                    // 应用裁剪
                    val startTimeStr = etStartTime.text.toString()
                    val endTimeStr = etEndTime.text.toString()
                    
                    val startTimeMs = if (startTimeStr.isEmpty()) 0L else parseTimeString(startTimeStr)
                    val endTimeMs = if (endTimeStr.isEmpty()) {
                        viewModel.mediaDuration.value ?: 0L
                    } else {
                        parseTimeString(endTimeStr)
                    }
                    
                    if (startTimeMs >= 0 && endTimeMs > 0 && endTimeMs > startTimeMs) {
                        // 设置裁剪范围
                        viewModel.setTrimRange(startTimeMs, endTimeMs)
                        
                        // 显示成功消息
                        Toast.makeText(requireContext(), "已应用裁剪设置", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "请输入有效的时间范围", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察媒体时长变化
        viewModel.mediaDuration.observe(viewLifecycleOwner) { duration ->
            // 媒体时长变化时更新UI
            updateTrimRange()
        }
    }
    
    /**
     * 更新裁剪范围显示
     */
    private fun updateTrimRange() {
        val startMs = viewModel.getTrimStartMs()
        val endMs = viewModel.getTrimEndMs()
        
        val startTimeStr = formatMillisToTimeString(startMs)
        val endTimeStr = formatMillisToTimeString(endMs)
        
        binding.tvTrimRange.text = "$startTimeStr - $endTimeStr"
    }
    
    /**
     * 将时间字符串解析为毫秒值
     * 支持格式：HH:MM:SS 或 MM:SS
     */
    private fun parseTimeString(timeStr: String): Long {
        return try {
            val parts = timeStr.split(":")
            when (parts.size) {
                3 -> { // HH:MM:SS
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toLong()
                    
                    TimeUnit.HOURS.toMillis(hours) +
                            TimeUnit.MINUTES.toMillis(minutes) +
                            TimeUnit.SECONDS.toMillis(seconds)
                }
                2 -> { // MM:SS
                    val minutes = parts[0].toLong()
                    val seconds = parts[1].toLong()
                    
                    TimeUnit.MINUTES.toMillis(minutes) +
                            TimeUnit.SECONDS.toMillis(seconds)
                }
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * 将毫秒值格式化为时间字符串
     */
    private fun formatMillisToTimeString(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 分割点适配器
     */
    private inner class SplitPointAdapter : 
        BaseQuickAdapter<Long, ViewBindingHolder<ItemVideoBinding>>() {
        
        override fun onBindViewHolder(
            holder: ViewBindingHolder<ItemVideoBinding>,
            position: Int,
            item: Long?
        ) {
            val binding = holder.getBinding()
            item ?: return
            
            binding.tvTitle.text = "分割点 ${position + 1}: ${formatMillisToTimeString(item)}"
            binding.tvSubtitle.text = "点击移除"
            binding.ivThumbnail.setImageResource(android.R.drawable.ic_menu_crop)
        }
        
        override fun onCreateViewHolder(
            context: Context,
            parent: ViewGroup,
            viewType: Int
        ): ViewBindingHolder<ItemVideoBinding> {
            return ViewBindingHolder(
                ItemVideoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
    
    /**
     * 视频列表适配器
     */
    private inner class VideoAdapter : 
        BaseQuickAdapter<MediaInfo, ViewBindingHolder<ItemVideoBinding>>() {
        
        override fun onBindViewHolder(
            holder: ViewBindingHolder<ItemVideoBinding>,
            position: Int,
            item: MediaInfo?
        ) {
            val binding = holder.getBinding()
            item ?: return
            
            binding.tvTitle.text = item.name
            binding.tvSubtitle.text = "时长: ${item.getFormattedDuration()}"
            binding.ivThumbnail.load(item.path)
        }
        
        override fun onCreateViewHolder(
            context: Context,
            parent: ViewGroup,
            viewType: Int
        ): ViewBindingHolder<ItemVideoBinding> {
            return ViewBindingHolder(
                ItemVideoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
} 