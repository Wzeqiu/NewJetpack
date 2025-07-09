package com.common.kt

/**
 * 字符串扩展函数
 */

/**
 * 判断字符串是否为空或只包含空白字符
 */
fun String?.isNullOrAllBlank(): Boolean {
    return isNullOrEmpty() || all { it.isWhitespace() }
}

/**
 * 格式化时长为 MM:SS 或 HH:MM:SS 格式
 */
fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
} 