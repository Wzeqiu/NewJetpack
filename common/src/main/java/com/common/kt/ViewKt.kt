package com.common.kt

import android.annotation.SuppressLint
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

/**
 * 为View添加触摸缩放动画
 * 当手指按下时缩小，手指抬起或取消时放大
 */
@SuppressLint("ClickableViewAccessibility")
fun View.addTouchScaleAnimation(scale: Float = 0.9f, duration: Long = 100): View {
    setOnTouchListener { _, event ->
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN -> animate().scaleX(scale).scaleY(scale)
                .setDuration(duration).start()

            android.view.MotionEvent.ACTION_UP,
            android.view.MotionEvent.ACTION_CANCEL -> animate().scaleX(1.0f).scaleY(1.0f)
                .setDuration(duration).start()
        }
        false
    }
    return this
}