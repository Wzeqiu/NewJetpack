package com.wzeqiu.mediacode.editor.panel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.wzeqiu.mediacode.R
import com.wzeqiu.mediacode.editor.StickerOverlay
import com.wzeqiu.mediacode.editor.VideoEditorViewModel

/**
 * 贴纸面板Fragment
 * 
 * 提供贴纸选择、编辑和应用功能
 */
class StickerPanelFragment : Fragment() {

    private lateinit var viewModel: VideoEditorViewModel
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var controlPanel: View
    private lateinit var alphaSeekBar: SeekBar
    private lateinit var alphaValue: TextView
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var sizeValue: TextView
    
    // 当前选中的贴纸
    private var currentStickerOverlay: StickerOverlay? = null
    
    // 贴纸资源列表（按类别分组）
    private val emojiStickers = listOf(
        R.drawable.ic_sticker_emoji_smile,
        R.drawable.ic_sticker_emoji_sad,
        R.drawable.ic_sticker_emoji_love,
//        R.drawable.ic_sticker_emoji_cool,
//        R.drawable.ic_sticker_emoji_angry,
//        R.drawable.ic_sticker_emoji_surprise,
//        R.drawable.ic_sticker_emoji_wink,
//        R.drawable.ic_sticker_emoji_laugh
    )
    
    private val decorationStickers = listOf(
        R.drawable.ic_sticker_decoration_star,
        R.drawable.ic_sticker_decoration_heart,
//        R.drawable.ic_sticker_decoration_flower,
//        R.drawable.ic_sticker_decoration_crown,
//        R.drawable.ic_sticker_decoration_balloon,
//        R.drawable.ic_sticker_decoration_cake,
//        R.drawable.ic_sticker_decoration_gift,
//        R.drawable.ic_sticker_decoration_ribbon
    )
    
    private val effectStickers = listOf(
        R.drawable.ic_sticker_effect_fire,
        R.drawable.ic_sticker_effect_snow,
//        R.drawable.ic_sticker_effect_rain,
//        R.drawable.ic_sticker_effect_sparkle,
//        R.drawable.ic_sticker_effect_confetti,
//        R.drawable.ic_sticker_effect_lightning,
//        R.drawable.ic_sticker_effect_bubble,
//        R.drawable.ic_sticker_effect_smoke
    )
    
    // 当前显示的贴纸列表
    private var currentStickers = emojiStickers
    
    companion object {
        fun newInstance() = StickerPanelFragment()
        
        // 贴纸分类索引
        private const val TAB_EMOJI = 0
        private const val TAB_DECORATION = 1
        private const val TAB_EFFECT = 2
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.panel_sticker, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[VideoEditorViewModel::class.java]
        
        initViews(view)
        setupTabLayout()
        setupStickersList()
        setupControlPanel()
        
        // 观察当前选中的贴纸
        viewModel.selectedStickerOverlay.observe(viewLifecycleOwner) { sticker ->
            updateSelectedSticker(sticker)
        }
    }
    
    /**
     * 初始化视图组件
     */
    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerView)
        controlPanel = view.findViewById(R.id.controlPanel)
        alphaSeekBar = view.findViewById(R.id.alphaSeekBar)
        alphaValue = view.findViewById(R.id.alphaValue)
        sizeSeekBar = view.findViewById(R.id.sizeSeekBar)
        sizeValue = view.findViewById(R.id.sizeValue)
        
        view.findViewById<View>(R.id.btnDelete).setOnClickListener {
            currentStickerOverlay?.let {
                viewModel.removeStickerOverlay(it)
                controlPanel.visibility = View.GONE
            }
        }
        
        view.findViewById<View>(R.id.btnApply).setOnClickListener {
            applyCurrentStickerChanges()
            controlPanel.visibility = View.GONE
        }
    }
    
    /**
     * 设置贴纸分类选项卡
     */
    private fun setupTabLayout() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    TAB_EMOJI -> {
                        currentStickers = emojiStickers
                        updateStickersList(StickerOverlay.CATEGORY_EMOJI)
                    }
                    TAB_DECORATION -> {
                        currentStickers = decorationStickers
                        updateStickersList(StickerOverlay.CATEGORY_DECORATION)
                    }
                    TAB_EFFECT -> {
                        currentStickers = effectStickers
                        updateStickersList(StickerOverlay.CATEGORY_EFFECT)
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    /**
     * 设置贴纸列表
     */
    private fun setupStickersList() {
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        updateStickersList(StickerOverlay.CATEGORY_EMOJI)
    }
    
    /**
     * 更新贴纸列表（根据当前选择的分类）
     */
    private fun updateStickersList(category: String) {
        val adapter = StickerAdapter(currentStickers) { resourceId ->
            addStickerOverlay(resourceId, category)
        }
        recyclerView.adapter = adapter
    }
    
    /**
     * 添加贴纸覆盖层
     */
    private fun addStickerOverlay(resourceId: Int, category: String) {
        // 获取当前播放位置作为贴纸开始时间
        val currentPosition = viewModel.getCurrentPosition()
        val duration = viewModel.getMediaDuration()
        
        // 创建贴纸覆盖层（默认显示5秒）
        val endTimeMs = minOf(currentPosition + 5000, duration)
        
        val stickerOverlay = StickerOverlay(
            resourceId = resourceId,
            category = category,
            startTimeMs = currentPosition,
            endTimeMs = endTimeMs,
            xPosition = 0.5f, // 默认在中间
            yPosition = 0.5f,
            size = StickerOverlay.DEFAULT_SIZE,
            rotation = StickerOverlay.DEFAULT_ROTATION,
            alpha = StickerOverlay.DEFAULT_ALPHA
        )
        
        // 添加并选中贴纸
        viewModel.addStickerOverlay(stickerOverlay)
        viewModel.selectStickerOverlay(stickerOverlay)
        
        // 显示控制面板
        controlPanel.visibility = View.VISIBLE
        
        // 更新控制面板数值
        updateControlPanel(stickerOverlay)
    }
    
    /**
     * 设置贴纸控制面板
     */
    private fun setupControlPanel() {
        // 透明度控制
        alphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val percent = (progress * 100 / 255)
                alphaValue.text = "$percent%"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 大小控制
        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                sizeValue.text = "$progress%"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    /**
     * 更新选中的贴纸
     */
    private fun updateSelectedSticker(sticker: StickerOverlay?) {
        currentStickerOverlay = sticker
        
        if (sticker != null) {
            // 显示控制面板
            controlPanel.visibility = View.VISIBLE
            
            // 更新控制面板数值
            updateControlPanel(sticker)
        } else {
            // 隐藏控制面板
            controlPanel.visibility = View.GONE
        }
    }
    
    /**
     * 更新控制面板数值
     */
    private fun updateControlPanel(sticker: StickerOverlay) {
        // 设置透明度
        alphaSeekBar.progress = sticker.alpha
        val alphaPercent = (sticker.alpha * 100 / 255)
        alphaValue.text = "$alphaPercent%"
        
        // 设置大小
        val sizePercent = (sticker.size * 100).toInt()
        sizeSeekBar.progress = sizePercent
        sizeValue.text = "$sizePercent%"
    }
    
    /**
     * 应用当前贴纸更改
     */
    private fun applyCurrentStickerChanges() {
        currentStickerOverlay?.let { sticker ->
            // 创建更新后的贴纸
            val updatedSticker = sticker.copyWith(
                alpha = alphaSeekBar.progress,
                size = sizeSeekBar.progress / 100f
            )
            
            // 更新贴纸
            viewModel.updateStickerOverlay(updatedSticker)
        }
    }
    
    /**
     * 贴纸适配器
     */
    private inner class StickerAdapter(
        private val stickers: List<Int>,
        private val onStickerClick: (Int) -> Unit
    ) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {
        
        inner class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val stickerImageView: ImageView = itemView.findViewById(R.id.ivSticker)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sticker, parent, false)
            return StickerViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val resourceId = stickers[position]
            
            try {
                holder.stickerImageView.setImageResource(resourceId)
                holder.itemView.setOnClickListener {
                    onStickerClick(resourceId)
                }
            } catch (e: Exception) {
                // 资源加载失败，使用默认图标
                holder.stickerImageView.setImageResource(android.R.drawable.ic_menu_help)
            }
        }
        
        override fun getItemCount(): Int = stickers.size
    }
} 