package com.common.widget.guide

import android.app.Activity
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.common.common.R

/**
 * 引导页控制器，用于管理引导页的显示和交互
 */
class GuidePageController(private val activity: Activity) {

    private var overlayView: GuideOverlayView? = null
    private var contentContainer: FrameLayout? = null
    private var rootView: ViewGroup? = null
    private var currentStep = 0
    private var guideSteps = mutableListOf<GuideStep>()
    private var onFinishListener: (() -> Unit)? = null

    init {
        rootView = activity.window.decorView as ViewGroup
    }

    /**
     * 显示引导页
     */
    fun show() {
        if (guideSteps.isEmpty()) return
        currentStep = 0
        setupOverlayView()
        showCurrentStep()
    }

    /**
     * 隐藏引导页
     */
    fun dismiss() {
        rootView?.removeView(overlayView)
        rootView?.removeView(contentContainer)
        overlayView = null
        contentContainer = null
    }

    /**
     * 添加引导步骤
     * @param step 引导步骤
     */
    fun addStep(step: GuideStep) {
        guideSteps.add(step)
    }

    /**
     * 添加多个引导步骤
     * @param steps 引导步骤列表
     */
    fun addSteps(steps: List<GuideStep>) {
        guideSteps.addAll(steps)
    }

    /**
     * 清除所有引导步骤
     */
    fun clearSteps() {
        guideSteps.clear()
    }

    /**
     * 设置引导结束监听器
     * @param listener 结束回调
     */
    fun setOnFinishListener(listener: () -> Unit) {
        onFinishListener = listener
    }

    /**
     * 前往下一步
     */
    fun nextStep() {
        if (currentStep < guideSteps.size - 1) {
            currentStep++
            showCurrentStep()
        } else {
            dismiss()
            onFinishListener?.invoke()
        }
    }

    /**
     * 返回上一步
     */
    fun previousStep() {
        if (currentStep > 0) {
            currentStep--
            showCurrentStep()
        }
    }

    private fun setupOverlayView() {
        if (overlayView == null) {
            // 创建遮罩层
            overlayView = GuideOverlayView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setOnClickListener {
                    // 点击遮罩层进入下一步
                    nextStep()
                }
                isClickable=true
            }
            rootView?.addView(overlayView)

            // 创建内容容器
            contentContainer = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isClickable=true
            }
            rootView?.addView(contentContainer)
        }
    }

    private fun showCurrentStep() {
        val step = guideSteps.getOrNull(currentStep) ?: return

        // 更新遮罩层的穿透区域
        val targetRect = RectF()
        step.targetView?.let {
            overlayView?.setHoleForView(it, step.padding,step.margin, step.cornerRadius)
            
            // 计算目标视图在屏幕上的位置和大小，供自定义视图使用
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            val selfLocation = IntArray(2)
            overlayView?.getLocationOnScreen(selfLocation)
            
            targetRect.set(
                (location[0] - selfLocation[0] - step.padding.left + step.margin.left).toFloat(),
                (location[1] - selfLocation[1] - step.padding.top+step.margin.top).toFloat(),
                (location[0] - selfLocation[0] + it.width + step.padding.right- step.margin.right).toFloat(),
                (location[1] - selfLocation[1] + it.height + step.padding.bottom- step.margin.bottom).toFloat()
            )
        } ?: run {
            step.targetRect?.let {
                overlayView?.setHoleRect(it, step.cornerRadius)
                targetRect.set(it)
            } ?: overlayView?.clearHoles()
        }

        // 更新内容视图
        contentContainer?.removeAllViews()

        // 如果有自定义视图，根据透传区域位置调整其布局
        step.customContentView?.let {
            // 设置自定义视图的布局参数
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            
            // 如果有目标区域，根据其位置放置自定义视图
            if (!targetRect.isEmpty) {
                // 将目标区域信息通过tag传递给自定义视图，供其内部使用
                it.setTag(R.id.guide_target_rect, targetRect)
            }
            it.layoutParams = layoutParams
            contentContainer?.addView(it)
            return
        }
    }

    /**
     * 引导步骤数据类
     */
    data class GuideStep(
        val targetView: View? = null, // 目标视图
        val targetRect: RectF? = null, // 或者直接指定目标区域
        val customContentView: View? = null, // 自定义内容视图
        val padding: RectF = RectF(), // 穿透区域扩展的内边距
        val margin: RectF = RectF(), // 穿透区域扩展的外边距
        val cornerRadius: Float = 20f // 穿透区域圆角半径
    )
} 