package com.common.widget.video

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import androidx.media3.ui.PlayerView

/**
 * 自定义圆角Media3播放器视图
 * 支持设置圆角半径，实现视频播放器的圆角效果
 */
class RoundedExoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private val path = Path()
    private val rectF = RectF()
    
    // 圆角半径，默认为0（无圆角）
    var cornerRadius: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    // 是否单独设置每个角的圆角半径
    private var cornerRadiusTopLeft: Float = 0f
    private var cornerRadiusTopRight: Float = 0f
    private var cornerRadiusBottomLeft: Float = 0f
    private var cornerRadiusBottomRight: Float = 0f
    private var useIndividualCorners = false

    /**
     * 设置四个角的圆角半径
     */
    fun setCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        useIndividualCorners = true
        cornerRadiusTopLeft = topLeft
        cornerRadiusTopRight = topRight
        cornerRadiusBottomLeft = bottomLeft
        cornerRadiusBottomRight = bottomRight
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath(w, h)
    }

    private fun updatePath(width: Int, height: Int) {
        path.reset()
        rectF.set(0f, 0f, width.toFloat(), height.toFloat())
        
        if (useIndividualCorners) {
            // 使用不同的圆角半径
            val radii = floatArrayOf(
                cornerRadiusTopLeft, cornerRadiusTopLeft,
                cornerRadiusTopRight, cornerRadiusTopRight,
                cornerRadiusBottomRight, cornerRadiusBottomRight,
                cornerRadiusBottomLeft, cornerRadiusBottomLeft
            )
            path.addRoundRect(rectF, radii, Path.Direction.CW)
        } else {
            // 使用统一的圆角半径
            path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        }
    }

    override fun draw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.draw(canvas)
        canvas.restoreToCount(save)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.save()
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
        canvas.restoreToCount(save)
    }
} 