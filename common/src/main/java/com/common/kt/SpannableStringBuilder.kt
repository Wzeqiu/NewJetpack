package com.common.kt

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView

inline fun SpannableStringBuilder.click(
    textView: TextView,
    linkColor: Int? = Color.parseColor("#FF0000"),
    underLine: Boolean = false,
    noinline click: () -> Unit = {},
    builderAction: SpannableStringBuilder.() -> Unit
): SpannableStringBuilder {
    textView.movementMethod = LinkMovementMethod.getInstance()
    textView.highlightColor = Color.TRANSPARENT
    if (linkColor != null) {
        textView.setLinkTextColor(linkColor)
    }
    
    val start = length
    builderAction()
    val end = length
    
    if (end > start) {
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                click.invoke()
            }

            override fun updateDrawState(ds: TextPaint) {
                // 只有当没有设置ForegroundColorSpan时才设置颜色
                // 这样可以保留渐变色效果
                if (linkColor != null && !hasSpan(android.text.style.ForegroundColorSpan::class.java, start, end)) {
                    ds.color = ds.linkColor
                }
                ds.isUnderlineText = underLine
            }
        }
        
        // 使用SPAN_INCLUSIVE_INCLUSIVE以确保与其他span兼容
        setSpan(
            clickableSpan,
            start,
            end,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    
    return this
}

/**
 * 检查指定范围内是否存在特定类型的Span
 */
 fun <T> SpannableStringBuilder.hasSpan(spanClass: Class<T>, start: Int, end: Int): Boolean {
    val spans = getSpans(start, end, spanClass)
    return spans.isNotEmpty()
}

/**
 * 为文本添加渐变色效果
 * @param startColor 渐变起始颜色
 * @param endColor 渐变结束颜色
 * @param text 要显示的文本
 * @return SpannableStringBuilder
 */
fun SpannableStringBuilder.gradientText(
    startColor: Int,
    endColor: Int,
    text: String
): SpannableStringBuilder {
    val start = length
    append(text)
    val end = length
    
    if (end > start) {
        val textLength = end - start
        for (i in 0 until textLength) {
            val ratio = i.toFloat() / (textLength - 1).coerceAtLeast(1)
            val red = Color.red(startColor) + ((Color.red(endColor) - Color.red(startColor)) * ratio).toInt()
            val green = Color.green(startColor) + ((Color.green(endColor) - Color.green(startColor)) * ratio).toInt()
            val blue = Color.blue(startColor) + ((Color.blue(endColor) - Color.blue(startColor)) * ratio).toInt()
            val alpha = Color.alpha(startColor) + ((Color.alpha(endColor) - Color.alpha(startColor)) * ratio).toInt()
            val color = Color.argb(alpha, red, green, blue)
            
            setSpan(
                android.text.style.ForegroundColorSpan(color),
                start + i,
                start + i + 1,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    
    return this
}

/**
 * 为文本添加渐变色效果
 * @param startColor 渐变起始颜色
 * @param endColor 渐变结束颜色
 * @param builderAction 文本构建操作
 * @return SpannableStringBuilder
 */
fun SpannableStringBuilder.gradient(
    startColor: Int,
    endColor: Int,
    builderAction: SpannableStringBuilder.() -> Unit
): SpannableStringBuilder {
    // 保存当前长度作为开始位置
    val start = length
    
    // 运行构建器操作添加文本
    this.builderAction()
    
    // 获取结束位置
    val end = length
    
    // 确保有新添加的文本
    if (end > start) {
        // 获取文本长度
        val textLength = end - start
        
        // 对每个字符应用渐变色
        for (i in 0 until textLength) {
            // 计算当前位置的渐变比例
            val ratio = if (textLength > 1) i.toFloat() / (textLength - 1) else 0f
            
            // 计算渐变颜色的RGB分量
            val red = (Color.red(startColor) + (Color.red(endColor) - Color.red(startColor)) * ratio).toInt().coerceIn(0, 255)
            val green = (Color.green(startColor) + (Color.green(endColor) - Color.green(startColor)) * ratio).toInt().coerceIn(0, 255)
            val blue = (Color.blue(startColor) + (Color.blue(endColor) - Color.blue(startColor)) * ratio).toInt().coerceIn(0, 255)
            
            // 创建渐变颜色
            val color = Color.rgb(red, green, blue)
            
            // 应用前景色Span，使用SPAN_INCLUSIVE_INCLUSIVE以确保与其他span兼容
            setSpan(
                android.text.style.ForegroundColorSpan(color),
                start + i,
                start + i + 1,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    
    return this
}

/**
 * 为文本添加点击事件和渐变色效果
 * @param textView 目标TextView
 * @param startColor 渐变起始颜色
 * @param endColor 渐变结束颜色
 * @param linkColor 链接颜色，为null时使用渐变色
 * @param underLine 是否显示下划线
 * @param click 点击事件回调
 * @param text 要显示的文本
 * @return SpannableStringBuilder
 */
fun SpannableStringBuilder.clickWithGradient(
    textView: TextView,
    startColor: Int,
    endColor: Int,
    linkColor: Int? = null,
    underLine: Boolean = false,
    click: () -> Unit = {},
    text: String
): SpannableStringBuilder {
    textView.movementMethod = LinkMovementMethod.getInstance()
    textView.highlightColor = Color.TRANSPARENT
    
    val start = length
    append(text)
    val end = length
    
    // 应用渐变色
    for (i in 0 until (end - start)) {
        val ratio = i.toFloat() / ((end - start) - 1).coerceAtLeast(1)
        val red = Color.red(startColor) + ((Color.red(endColor) - Color.red(startColor)) * ratio).toInt()
        val green = Color.green(startColor) + ((Color.green(endColor) - Color.green(startColor)) * ratio).toInt()
        val blue = Color.blue(startColor) + ((Color.blue(endColor) - Color.blue(startColor)) * ratio).toInt()
        val color = Color.rgb(red, green, blue)
        
        setSpan(
            android.text.style.ForegroundColorSpan(color),
            start + i,
            start + i + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    
    // 应用点击事件
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            click.invoke()
        }

        override fun updateDrawState(ds: TextPaint) {
            if (linkColor != null) {
                ds.color = linkColor
            }
            ds.isUnderlineText = underLine
        }
    }
    
    setSpan(
        clickableSpan, 
        start, 
        end, 
        android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    )
    
    return this
}


