package com.wzeqiu.mediacode.editor.panel

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.common.kt.isNullOrAllBlank
import com.common.kt.toastShort
import com.wzeqiu.mediacode.databinding.PanelTextBinding
import com.wzeqiu.mediacode.editor.TextOverlay
import com.wzeqiu.mediacode.editor.VideoEditorViewModel

/**
 * 文字/字幕编辑面板Fragment
 * 
 * 提供文字/字幕添加和编辑功能
 */
class TextPanelFragment : Fragment() {
    
    private lateinit var binding: PanelTextBinding
    private lateinit var viewModel: VideoEditorViewModel
    
    // 当前选择的文字颜色
    private var selectedTextColor: Int = Color.WHITE
    
    // 当前字体大小（像素）
    private var fontSize: Float = TextOverlay.DEFAULT_FONT_SIZE
    
    // 是否有背景
    private var hasBackground: Boolean = true
    
    // 位置信息
    private var xPosition: Float = TextOverlay.POSITION_CENTER
    private var yPosition: Float = TextOverlay.POSITION_CENTER
    
    // 选中的颜色View
    private var selectedColorView: View? = null
    
    companion object {
        fun newInstance(): TextPanelFragment {
            return TextPanelFragment()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PanelTextBinding.inflate(inflater, container, false)
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
    }
    
    /**
     * 初始化UI
     */
    private fun initUI() {
        // 设置预览文字
        binding.tvTextPreview.text = "预览文字"
        
        // 初始默认选择白色
        setSelectedColor(binding.colorWhite, Color.WHITE)
        
        // 设置字体大小显示
        binding.tvFontSize.text = "${fontSize.toInt()}px"
        binding.seekBarFontSize.progress = 50
        
        // 设置背景开关状态
        binding.switchTextBackground.isChecked = hasBackground
        updatePreviewBackground()
        
        // 设置时间默认值
        val mediaDuration = viewModel.mediaDuration.value ?: 0
        binding.editStartTime.setText("0")
        binding.editEndTime.setText("${mediaDuration / 1000}")
    }
    
    /**
     * 初始化监听器
     */
    private fun initListeners() {
        // 文本输入监听
        binding.editTextContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvTextPreview.text = s?.toString() ?: ""
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // 字体大小监听
        binding.seekBarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 将进度转换为字体大小（12-72像素）
                fontSize = 12f + (progress / 100f) * 60f
                binding.tvFontSize.text = "${fontSize.toInt()}px"
                
                // 更新预览
                binding.tvTextPreview.textSize = fontSize / 3 // 预览区域较小，除以3使显示合适
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 颜色选择监听
        val colorViews = listOf(
            binding.colorWhite,
            binding.colorBlack,
            binding.colorRed,
            binding.colorYellow,
            binding.colorBlue,
            binding.colorGreen,
            binding.colorPink,
            binding.colorPurple
        )
        
        val colors = listOf(
            Color.WHITE,
            Color.BLACK,
            Color.RED,
            Color.YELLOW,
            Color.BLUE,
            Color.GREEN,
            Color.parseColor("#E91E63"), // 粉色
            Color.parseColor("#9C27B0")  // 紫色
        )
        
        for (i in colorViews.indices) {
            colorViews[i].setOnClickListener {
                setSelectedColor(it, colors[i])
            }
        }
        
        // 背景开关监听
        binding.switchTextBackground.setOnCheckedChangeListener { _, isChecked ->
            hasBackground = isChecked
            updatePreviewBackground()
        }
        
        // 位置按钮监听
        binding.btnPositionTop.setOnClickListener {
            xPosition = TextOverlay.POSITION_CENTER
            yPosition = TextOverlay.POSITION_TOP
            updatePreviewPosition()
        }
        
        binding.btnPositionCenter.setOnClickListener {
            xPosition = TextOverlay.POSITION_CENTER
            yPosition = TextOverlay.POSITION_CENTER
            updatePreviewPosition()
        }
        
        binding.btnPositionBottom.setOnClickListener {
            xPosition = TextOverlay.POSITION_CENTER
            yPosition = TextOverlay.POSITION_BOTTOM
            updatePreviewPosition()
        }
        
        // 添加文字按钮监听
        binding.btnAddText.setOnClickListener {
            addTextOverlay()
        }
    }
    
    /**
     * 设置选中的颜色
     */
    private fun setSelectedColor(colorView: View, color: Int) {
        // 清除之前选中的边框
        selectedColorView?.setBackgroundColor(selectedTextColor)
        
        // 设置新选中的颜色
        selectedTextColor = color
        selectedColorView = colorView
        
        // 添加边框效果（这里简单处理，实际应该设置drawable边框）
        colorView.setBackgroundColor(color)
        
        // 更新预览文字颜色
        binding.tvTextPreview.setTextColor(color)
    }
    
    /**
     * 更新预览背景
     */
    private fun updatePreviewBackground() {
        if (hasBackground) {
            binding.tvTextPreview.setBackgroundColor(Color.argb(128, 0, 0, 0))
        } else {
            binding.tvTextPreview.setBackgroundColor(Color.TRANSPARENT)
        }
    }
    
    /**
     * 更新预览位置
     */
    private fun updatePreviewPosition() {
        val container = binding.textPreviewContainer
        val textView = binding.tvTextPreview
        
        // 简单处理，实际情况应该考虑更多因素
        when {
            yPosition == TextOverlay.POSITION_TOP -> {
                textView.translationY = -container.height * 0.3f
            }
            yPosition == TextOverlay.POSITION_CENTER -> {
                textView.translationY = 0f
            }
            yPosition == TextOverlay.POSITION_BOTTOM -> {
                textView.translationY = container.height * 0.3f
            }
        }
    }
    
    /**
     * 添加文字覆盖层
     */
    private fun addTextOverlay() {
        // 获取文字内容
        val text = binding.editTextContent.text.toString()
        if (text.isNullOrAllBlank()) {
            context?.toastShort("请输入文字内容")
            return
        }
        
        // 获取时间范围
        val startTimeStr = binding.editStartTime.text.toString()
        val endTimeStr = binding.editEndTime.text.toString()
        
        if (startTimeStr.isNullOrAllBlank() || endTimeStr.isNullOrAllBlank()) {
            context?.toastShort("请设置显示时间")
            return
        }
        
        try {
            val startTimeSec = startTimeStr.toFloat()
            val endTimeSec = endTimeStr.toFloat()
            
            if (startTimeSec >= endTimeSec) {
                context?.toastShort("结束时间必须大于开始时间")
                return
            }
            
            // 创建文字覆盖层对象
            val textOverlay = TextOverlay(
                text = text,
                startTimeMs = (startTimeSec * 1000).toLong(),
                endTimeMs = (endTimeSec * 1000).toLong(),
                xPosition = xPosition,
                yPosition = yPosition,
                fontSize = fontSize,
                textColor = selectedTextColor,
                hasBackground = hasBackground
            )
            
            // 添加到ViewModel
            viewModel.addTextOverlay(textOverlay)
            
            // 显示提示
            context?.toastShort("文字已添加")
            
            // 清空输入
            binding.editTextContent.setText("")
        } catch (e: Exception) {
            context?.toastShort("时间格式错误")
        }
    }
} 