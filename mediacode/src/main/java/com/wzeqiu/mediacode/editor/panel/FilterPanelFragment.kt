package com.wzeqiu.mediacode.editor.panel

import android.content.Context
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
import com.chad.library.adapter4.BaseQuickAdapter
import com.common.common.databinding.ItemMediaBinding
import com.common.kt.adapter.ViewBindingHolder
import com.common.kt.toastShort
import com.google.android.material.tabs.TabLayout
import com.wzeqiu.mediacode.R
import com.wzeqiu.mediacode.databinding.ItemFilterBinding
import com.wzeqiu.mediacode.databinding.PanelFilterBinding
import com.wzeqiu.mediacode.editor.VideoEditorViewModel
import com.wzeqiu.mediacode.editor.VideoFilter

/**
 * 滤镜面板Fragment
 *
 * 提供视频滤镜和特效选择功能
 */
class FilterPanelFragment : Fragment() {

    private lateinit var binding: PanelFilterBinding
    private lateinit var viewModel: VideoEditorViewModel

    // 滤镜适配器
    private val filterAdapter = FilterAdapter()

    // 当前选中的滤镜
    private var selectedFilter: VideoFilter? = null

    // 滤镜强度（0-100）
    private var filterIntensity: Int = 100

    // 所有滤镜，按类别分组
    private val allFilters = mapOf(
        "color" to listOf(
            VideoFilter("original", "原始", VideoFilter.FilterType.COLOR),
            VideoFilter("warm", "暖色调", VideoFilter.FilterType.COLOR),
            VideoFilter("cool", "冷色调", VideoFilter.FilterType.COLOR),
            VideoFilter("grayscale", "黑白", VideoFilter.FilterType.COLOR),
            VideoFilter("sepia", "怀旧", VideoFilter.FilterType.COLOR),
            VideoFilter("vivid", "鲜艳", VideoFilter.FilterType.COLOR)
        ),
        "effect" to listOf(
            VideoFilter("blur", "模糊", VideoFilter.FilterType.EFFECT),
            VideoFilter("sharpen", "锐化", VideoFilter.FilterType.EFFECT),
            VideoFilter("vignette", "暗角", VideoFilter.FilterType.EFFECT),
            VideoFilter("film_grain", "胶片颗粒", VideoFilter.FilterType.EFFECT),
            VideoFilter("mirror", "镜像", VideoFilter.FilterType.EFFECT)
        ),
        "mood" to listOf(
            VideoFilter("vintage", "复古", VideoFilter.FilterType.COLOR),
            VideoFilter("dramatic", "戏剧", VideoFilter.FilterType.COLOR),
            VideoFilter("happy", "明快", VideoFilter.FilterType.COLOR),
            VideoFilter("sad", "忧郁", VideoFilter.FilterType.COLOR),
            VideoFilter("dream", "梦幻", VideoFilter.FilterType.COLOR)
        )
    )

    companion object {
        fun newInstance(): FilterPanelFragment {
            return FilterPanelFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PanelFilterBinding.inflate(inflater, container, false)
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

        // 初始化滤镜类别
        initFilterCategories()
    }

    /**
     * 初始化UI
     */
    private fun initUI() {
        // 设置滤镜列表
        binding.recyclerFilters.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = filterAdapter
        }

        // 设置强度显示
        binding.tvIntensity.text = "$filterIntensity%"
        binding.seekBarIntensity.progress = filterIntensity
    }

    /**
     * 初始化监听器
     */
    private fun initListeners() {
        // 强度调整
        binding.seekBarIntensity.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                filterIntensity = progress
                binding.tvIntensity.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 应用滤镜按钮
        binding.btnApplyFilter.setOnClickListener {
            applyFilter()
        }
    }

    /**
     * 初始化滤镜类别
     */
    private fun initFilterCategories() {
        // 添加类别标签
        binding.tabFilterCategory.apply {
            addTab(newTab().setText("色彩").setTag("color"))
            addTab(newTab().setText("特效").setTag("effect"))
            addTab(newTab().setText("氛围").setTag("mood"))

            // 类别切换监听
            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val category = tab?.tag as? String ?: "color"
                    updateFilterList(category)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {}

                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }

        // 默认显示色彩滤镜
        updateFilterList("color")
    }

    /**
     * 更新滤镜列表
     */
    private fun updateFilterList(category: String) {
        // 获取指定类别的滤镜
        val filters = allFilters[category] ?: emptyList()

        // 更新适配器数据
        filterAdapter.submitList(filters)

        // 清除选中状态
        selectedFilter = null
    }

    /**
     * 应用滤镜
     */
    private fun applyFilter() {
        if (selectedFilter == null) {
            context?.toastShort("请选择一个滤镜")
            return
        }

        selectedFilter?.let { filter ->
            // 设置强度属性到滤镜对象
            val enhancedFilter = VideoFilter(
                id = filter.id,
                name = filter.name,
                type = filter.type
            )

            // 应用滤镜
            viewModel.applyFilter(enhancedFilter)

            // 显示提示
            context?.toastShort("已应用${filter.name}滤镜")
        }
    }

    /**
     * 滤镜适配器
     */
    inner class FilterAdapter :
        BaseQuickAdapter<VideoFilter, ViewBindingHolder<ItemFilterBinding>>() {

        override fun onCreateViewHolder(
            context: Context,
            parent: ViewGroup,
            viewType: Int
        ): ViewBindingHolder<ItemFilterBinding> {
            return ViewBindingHolder(
                ItemFilterBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(
            holder: ViewBindingHolder<ItemFilterBinding>,
            position: Int,
            item: VideoFilter?
        ) {
            item?.let { filter ->
                holder.getBinding().apply {
                    // 设置滤镜名称
                    tvFilterName.text = filter.name

                    // 设置滤镜预览图（在实际应用中，这里应该是应用滤镜后的缩略图）
                    // 这里使用简单的占位图模拟不同滤镜效果
                    val imageResource = when (filter.id) {
                        "original" -> R.drawable.ic_launcher_background
                        "grayscale" -> R.drawable.ic_launcher_background // 实际应用中应该有不同的预览图
                        "sepia" -> R.drawable.ic_launcher_background
                        else -> R.drawable.ic_launcher_background
                    }
                    ivFilterPreview.setImageResource(imageResource)

                    // 设置点击监听
                    root.setOnClickListener {
                        // 更新选中的滤镜
                        selectedFilter = filter
                        notifyDataSetChanged()

                        // 显示选中状态
                        context.toastShort("已选择${filter.name}滤镜")
                    }

                    // 显示选中状态
                    root.alpha = if (filter == selectedFilter) 1.0f else 0.7f
                }
            }
        }
    }
} 