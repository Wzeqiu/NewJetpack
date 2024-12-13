package com.common.kt

import android.graphics.Color
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.core.text.inSpans

inline fun SpannableStringBuilder.click(
    textView: TextView,
    linkColor: Int? = Color.parseColor("#FF0000"),
    underLine: Boolean = false,
    noinline click: () -> Unit = {},
    builderAction: SpannableStringBuilder.() -> Unit
): SpannableStringBuilder {
    textView.movementMethod = LinkMovementMethod.getInstance()
    textView.highlightColor = Color.TRANSPARENT
    textView.setLinkTextColor(linkColor ?: textView.textColors.defaultColor)
    val clickableSpan = object : ClickableSpan() {
        override fun onClick(widget: View) {
            click.invoke()
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.color = ds.linkColor
            ds.isUnderlineText = underLine
        }
    }
    return inSpans(clickableSpan, builderAction = builderAction)
}


