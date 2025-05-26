package com.common.widget.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.Choreographer
import com.common.common.R
import java.util.concurrent.TimeUnit

/**
 * 自定义倒计时View，显示时、分、秒、毫秒
 */
class CountdownView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint对象用于绘制
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 时间数值
    private var hours: Long = 0
    private var minutes: Long = 0
    private var seconds: Long = 0
    private var millis: Long = 0

    // 控件尺寸相关
    private var segmentWidth: Float = 0f
    private var segmentHeight: Float = 0f
    private val segmentPadding: Float = 5f // dp
    private val cornerRadius: Float = 5f // dp
    private val textSize: Float = 20f // sp
    private val separatorText: String = ":"
    private var separatorWidth: Float = 0f

    // 颜色
    private var textColor = Color.WHITE
    private var segmentColor = Color.parseColor("#FF5722") // 示例红色
    private var separatorColor = Color.WHITE

    private var totalMillis: Long = 0
    private var startTime: Long = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val elapsedMillis = System.currentTimeMillis() - startTime
            val remainingMillis = (totalMillis - elapsedMillis).coerceAtLeast(0)
            updateDisplayTime(remainingMillis)

            if (remainingMillis > 0) {
                Choreographer.getInstance().postFrameCallback (this)
            } else {
                // 倒计时结束逻辑
            }
        }
    }

    init {
        // 从XML属性初始化
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CountdownView, 0, 0)
            textColor = typedArray.getColor(R.styleable.CountdownView_countdown_textColor, Color.WHITE)
            segmentColor = typedArray.getColor(
                R.styleable.CountdownView_countdown_segmentColor,
                Color.parseColor("#FF5722")
            )
            separatorColor =
                typedArray.getColor(R.styleable.CountdownView_countdown_separatorColor, Color.BLUE)
            // TypedArray使用后需要回收
            typedArray.recycle()
        }

        // 初始化Paint
        textPaint.color = textColor
        textPaint.textSize = spToPx(textSize)
        textPaint.textAlign = Paint.Align.CENTER

        segmentPaint.color = segmentColor

        separatorPaint.color = separatorColor
        separatorPaint.textSize = spToPx(textSize)
        separatorPaint.textAlign = Paint.Align.CENTER

        separatorWidth = separatorPaint.measureText(separatorText)
    }

    /**
     * 设置倒计时总时长
     * @param millis 时长，单位毫秒
     */
    fun setTime(millis: Long) {
        this.totalMillis = millis
        updateDisplayTime(millis)
        // 不在这里启动，等待start调用
        start()
    }

    /**
     * 开始倒计时
     */
    fun start() {
        if (totalMillis <= 0) return
        stop() // 先停止之前的回调
        startTime = System.currentTimeMillis()
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    /**
     * 停止倒计时
     */
    fun stop() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    /**
     * 更新显示的时间
     * @param millisRemaining 剩余毫秒数
     */
    private fun updateDisplayTime(millisRemaining: Long) {
        hours = TimeUnit.MILLISECONDS.toHours(millisRemaining)
        minutes = TimeUnit.MILLISECONDS.toMinutes(millisRemaining) % 60
        seconds = TimeUnit.MILLISECONDS.toSeconds(millisRemaining) % 60
        millis = (millisRemaining % 1000) / 10 // 显示两位毫秒
        invalidate() // 请求重绘
    }

    /**
     * 测量View大小
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 简单计算
        val textHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top
        segmentHeight = textHeight + 2 * dpToPx(segmentPadding)
        // 假设每个数字块宽度一致，以"00"为基准
        segmentWidth = textPaint.measureText("00") + 2 * dpToPx(segmentPadding)

        val desiredWidth = (segmentWidth * 4) + (separatorWidth * 3) + (dpToPx(segmentPadding) * 6) // 4个数字块，3个分隔符，左右padding
        val desiredHeight = segmentHeight

        setMeasuredDimension(
            resolveSize(desiredWidth.toInt(), widthMeasureSpec),
            resolveSize(desiredHeight.toInt(), heightMeasureSpec)
        )
    }

    /**
     * 绘制View内容
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        var currentX = 0f// 起始X坐标

        // 绘制小时
        drawSegment(canvas, formatTime(hours), currentX, textY)
        currentX += segmentWidth + dpToPx(segmentPadding)
        drawSeparator(canvas, currentX, textY)
        currentX += separatorWidth + dpToPx(segmentPadding)

        // 绘制分钟
        drawSegment(canvas, formatTime(minutes), currentX, textY)
        currentX += segmentWidth + dpToPx(segmentPadding)
        drawSeparator(canvas, currentX, textY)
        currentX += separatorWidth + dpToPx(segmentPadding)

        // 绘制秒
        drawSegment(canvas, formatTime(seconds), currentX, textY)
        currentX += segmentWidth + dpToPx(segmentPadding)
        drawSeparator(canvas, currentX, textY)
        currentX += separatorWidth + dpToPx(segmentPadding)

        // 绘制毫秒
        drawSegment(canvas, formatTime(millis), currentX, textY)
    }

    /**
     * 绘制单个时间块
     * @param canvas 画布
     * @param text 时间文本
     * @param x 左上角x坐标
     * @param textY 文本的基线y坐标
     */
    private fun drawSegment(canvas: Canvas, text: String, x: Float, textY: Float) {
        val rect = RectF(x, 0f, x + segmentWidth, segmentHeight)
        canvas.drawRoundRect(rect, dpToPx(cornerRadius), dpToPx(cornerRadius), segmentPaint)
        canvas.drawText(text, x + segmentWidth / 2, textY, textPaint)
    }

    /**
     * 绘制分隔符
     * @param canvas 画布
     * @param x 分隔符中心x坐标
     * @param textY 文本的基线y坐标
     */
    private fun drawSeparator(canvas: Canvas, x: Float, textY: Float) {
        canvas.drawText(separatorText, x + separatorWidth / 2, textY, separatorPaint)
    }

    /**
     * 格式化时间数字，确保两位数显示
     * @param time 时间值
     * @return 格式化后的字符串
     */
    private fun formatTime(time: Long): String {
        return String.format("%02d", time)
    }

    // 工具方法：dp转px
    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    // 工具方法：sp转px
    private fun spToPx(sp: Float): Float {
        return sp * resources.displayMetrics.scaledDensity
    }
}