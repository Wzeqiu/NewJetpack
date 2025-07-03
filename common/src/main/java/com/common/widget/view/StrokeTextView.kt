package com.common.widget.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.common.common.R
import kotlin.math.max

/**
 * 描边文字控件
 * 支持自定义描边颜色、宽度和字间距
 */
class StrokeTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    // 描边属性
    private var strokeColor: Int = Color.BLACK
    private var strokeWidth: Float = 5f
    private var letterSpacing: Float = 0f

    // 描边画笔
    private val strokePaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
    }

    init {
        // 读取自定义属性
        attrs?.let {
            context.obtainStyledAttributes(it, R.styleable.StrokeTextView).apply {
                try {
                    strokeColor = getColor(R.styleable.StrokeTextView_strokeColor, Color.BLACK)
                    strokeWidth = getDimension(R.styleable.StrokeTextView_strokeWidth, 5f)

                    // 读取字间距属性
                    val customLetterSpacing = getFloat(R.styleable.StrokeTextView_textLetterSpacing, -1f)

                    // 设置字间距
                    letterSpacing = if (customLetterSpacing >= 0) {
                        customLetterSpacing
                    } else {
                        calculateLetterSpacing()
                    }
                } finally {
                    recycle()
                }
            }
        }

        // 初始化描边画笔
        strokePaint.apply {
            strokeWidth = this@StrokeTextView.strokeWidth
            color = strokeColor
            textSize = this@StrokeTextView.textSize
        }

        // 设置字间距
        setLetterSpacing(letterSpacing)
    }

    /**
     * 计算合适的字间距
     */
    private fun calculateLetterSpacing(): Float =
        max(strokeWidth / (textSize * 8), 0.01f)

    override fun onDraw(canvas: Canvas) {
        // 获取文本内容
        val textContent = text.toString()

        // 更新描边画笔属性
        strokePaint.apply {
            typeface = paint.typeface
            textSize = this@StrokeTextView.textSize
            letterSpacing = paint.letterSpacing
        }

        // 保存画布状态
        canvas.save().also { saveCount ->
            // 绘制描边文字
            layout?.let { textLayout ->
                // 多行文本处理
                for (i in 0 until textLayout.lineCount) {
                    val lineStart = textLayout.getLineStart(i)
                    val lineEnd = textLayout.getLineEnd(i)
                    val line = textContent.substring(lineStart, lineEnd)
                    val lineBaseline = textLayout.getLineBaseline(i).toFloat()
                    val lineLeft = textLayout.getLineLeft(i)

                    canvas.drawText(line, lineLeft + totalPaddingLeft, lineBaseline, strokePaint)
                }
            } ?: run {
                // 单行文本处理
                canvas.drawText(textContent, totalPaddingLeft.toFloat(), baseline.toFloat(), strokePaint)
            }

            // 恢复画布状态
            canvas.restoreToCount(saveCount)
        }

        // 绘制原始文字
        super.onDraw(canvas)
    }

    /**
     * 设置描边颜色
     */
    fun setStrokeColor(color: Int) {
        strokeColor = color
        strokePaint.color = color
        invalidate()
    }

    /**
     * 设置描边宽度
     */
    fun setStrokeWidth(width: Float) {
        strokeWidth = width
        strokePaint.strokeWidth = width

        // 自动调整字间距
        if (letterSpacing < 0) {
            setLetterSpacing(calculateLetterSpacing())
        }

        invalidate()
    }

    /**
     * 设置字间距
     */
    fun setCustomLetterSpacing(spacing: Float) {
        letterSpacing = spacing
        setLetterSpacing(spacing)
        invalidate()
    }

    override fun setTextSize(size: Float) {
        super.setTextSize(size)
        strokePaint.textSize = textSize

        // 更新字间距
        if (letterSpacing < 0 && strokeWidth > 0) {
            setLetterSpacing(calculateLetterSpacing())
        }
    }

}
