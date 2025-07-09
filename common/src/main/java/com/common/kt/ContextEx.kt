package com.common.kt

import android.content.Context
import android.widget.Toast

/**
 * Context扩展函数
 */

/**
 * 显示短时间Toast
 */
fun Context?.toastShort(message: String) {
    this?.let {
        Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 显示长时间Toast
 */
fun Context?.toastLong(message: String) {
    this?.let {
        Toast.makeText(it, message, Toast.LENGTH_LONG).show()
    }
} 