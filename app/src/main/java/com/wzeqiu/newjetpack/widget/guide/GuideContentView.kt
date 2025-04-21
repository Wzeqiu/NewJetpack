package com.wzeqiu.newjetpack.widget.guide

import android.content.Context
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.wzeqiu.newjetpack.R

/**
 * 自定义引导内容视图，能够根据目标区域自适应调整
 */
class GuideContentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {


    private var onNextClickListener: (() -> Unit)? = null
    

    init {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.layout_guide_content, this, true)
        

    }
    /**
     * 设置下一步按钮点击监听器
     */
    fun setOnNextClickListener(listener: () -> Unit) {
        onNextClickListener = listener
    }

    /**
     * 根据目标区域调整内容布局和箭头方向
     */
    fun adjustByTargetRect(targetRect: RectF?) {
        if (targetRect == null) {
            return
        }
        
        // 获取屏幕尺寸
        val screenWidth = rootView.width
        val screenHeight = rootView.height


        // 设置内容容器的外边距，为箭头留出空间
        val params = layoutParams as MarginLayoutParams
        params.topMargin = targetRect.bottom.toInt()
        layoutParams = params
        
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        // 从tag中获取目标区域信息并调整布局
        val targetRect = getTag(R.id.guide_target_rect) as? RectF
        adjustByTargetRect(targetRect)
    }
} 