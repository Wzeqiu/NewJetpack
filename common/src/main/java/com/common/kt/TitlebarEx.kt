package com.common.kt

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.common.common.R


fun Activity.setTitleBarContent(
    title: CharSequence,
    rightText: CharSequence = "",
    rightAction: View.() -> Unit={},
    action: View.() -> Unit
) {
    setTitleContent(title)
    setTitleRightContent(rightText,action=rightAction)
    setTitleBack(action)
}


fun Activity.setTitleContent(title: CharSequence, color: Int = 0) {
    findViewById<TextView>(R.id.titleBarTitle)?.apply {
        text = title
        if (color > 0) {
            setTextColor(color)
        }

    }
}

fun Activity.setTitleRightContent(
    content: CharSequence,
    color: Int = 0,
    action: View.() -> Unit = {}
) {
    findViewById<TextView>(R.id.titleBaRight)?.apply {
        isVisible=true
        text = content
        if (color > 0) {
            setTextColor(color)
        }
        singleClick(action = action)
    }
}

fun Activity.setTitleBack(action: View.() -> Unit) {
    findViewById<ImageView>(R.id.titleBarBack)?.singleClick(action = action)
}