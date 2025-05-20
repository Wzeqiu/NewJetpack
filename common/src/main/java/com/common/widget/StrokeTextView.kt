package com.common.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.TextPaint
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView


/**
 * 描边文字
 */
class StrokeTextView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {
    private var strokeColor: Int = Color.parseColor("#FF0000")
    private val outlineTextView: AppCompatTextView by lazy { AppCompatTextView(context, attrs) }

    init {
        val paint: TextPaint = outlineTextView.paint
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 15f // 描边宽度
        outlineTextView.setTextColor(strokeColor) // 描边颜色


    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        outlineTextView.layoutParams = params
        super.setLayoutParams(params)

    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        outlineTextView.layout(left, top, right, bottom)
        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        outlineTextView.text = text
        outlineTextView.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        outlineTextView.draw(canvas)
        super.onDraw(canvas)
    }


}
