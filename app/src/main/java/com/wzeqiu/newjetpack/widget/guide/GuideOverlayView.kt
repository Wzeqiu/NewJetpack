package com.mxm.douying.widget.guide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/**
 * 引导页遮罩视图，可以穿透指定区域查看下层内容
 */
class GuideOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val transparentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val clipPath = Path()
    private var holeRects = mutableListOf<RectF>()
    private var holeRadius = 0f
    private var defaultRadius = 20f

    @ColorInt
    private var overlayColor = Color.parseColor("#B2000000")

    init {
        // 确保我们的视图是可以绘制的
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        // 需要离屏缓冲才能正确处理透明区域
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        // 绘制整个背景
        paint.color = overlayColor
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // 剪切穿透区域
        clipPath.reset()
        for (rect in holeRects) {
            val radius = if (holeRadius > 0) holeRadius else defaultRadius
            clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        }

        // 将穿透区域设置为透明
        canvas.drawPath(clipPath, transparentPaint)

        canvas.restoreToCount(layer)
    }

    /**
     * 设置需要穿透显示的矩形区域列表
     * @param rects 矩形区域列表
     * @param radius 圆角半径，默认为0（直角矩形）
     */
    fun setHoleRects(rects: List<RectF>, radius: Float = 0f) {
        this.holeRects.clear()
        this.holeRects.addAll(rects)
        this.holeRadius = radius
        invalidate()
    }

    /**
     * 设置单个穿透区域
     * @param rect 矩形区域
     * @param radius 圆角半径，默认为0（直角矩形）
     */
    fun setHoleRect(rect: RectF, radius: Float = 0f) {
        setHoleRects(listOf(rect), radius)
    }

    /**
     * 设置穿透区域为目标View的位置和大小
     * @param targetView 目标View
     * @param padding 可选的内边距，扩展穿透区域
     * @param radius 圆角半径，默认为0（直角矩形）
     */
    fun setHoleForView(targetView: View, padding: RectF = RectF(), margin: RectF = RectF(), radius: Float = 0f) {
        val location = IntArray(2)
        targetView.getLocationOnScreen(location)

        // 获取当前视图在屏幕上的位置
        val selfLocation = IntArray(2)
        getLocationOnScreen(selfLocation)

        // 计算相对位置
        val rect = RectF(
            (location[0] - selfLocation[0] - padding.left + margin.left).toFloat(),
            (location[1] - selfLocation[1] - padding.top+margin.top).toFloat(),
            (location[0] - selfLocation[0] + targetView.width + padding.right- margin.right).toFloat(),
            (location[1] - selfLocation[1] + targetView.height + padding.bottom- margin.bottom).toFloat()
        )

        setHoleRect(rect, radius)
    }

    /**
     * 设置遮罩层的颜色
     * @param color 颜色值，需要带透明度
     */
    fun setOverlayColor(@ColorInt color: Int) {
        this.overlayColor = color
        invalidate()
    }

    /**
     * 清除所有穿透区域
     */
    fun clearHoles() {
        holeRects.clear()
        invalidate()
    }
} 