package com.common.kt

import android.view.View
import com.common.common.R


/**
 * view点击事件
 */
inline fun View.singleClick(interval: Long = 500, crossinline action: View.() -> Unit) {
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        val lastClickTime = getTag(R.id.view_check_interval_id)?.let { it as Long } ?: 0
        if (currentTime - lastClickTime < interval) return@setOnClickListener
        setTag(R.id.view_check_interval_id, currentTime)
        action(this)
    }
}

/**
 * view检查点击事件
 */
inline fun View.checkSingleClick(interval: Long = 500, crossinline action: View.() -> Unit) {
    val currentTime = System.currentTimeMillis()
    val lastClickTime = getTag(R.id.view_check_interval_id)?.let { it as Long } ?: 0
    if (currentTime - lastClickTime < interval) return
    setTag(R.id.view_check_interval_id, currentTime)
    action(this)
}