package com.wzeqiu.mediacode.editor

import android.graphics.Color
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 文字覆盖层
 * 
 * 表示添加到视频上的文字/字幕，包含：
 * - 文字内容
 * - 显示时间
 * - 样式设置
 * - 位置信息
 */
@Parcelize
data class TextOverlay(
    // 文字ID
    val id: String = System.currentTimeMillis().toString(),
    
    // 文字内容
    val text: String,
    
    // 开始时间（毫秒）
    val startTimeMs: Long,
    
    // 结束时间（毫秒）
    val endTimeMs: Long,
    
    // 水平位置 (0.0-1.0) 表示在视频宽度上的比例位置
    val xPosition: Float,
    
    // 垂直位置 (0.0-1.0) 表示在视频高度上的比例位置
    val yPosition: Float,
    
    // 字体大小（像素）
    val fontSize: Float,
    
    // 文字颜色
    val textColor: Int,
    
    // 是否有背景
    val hasBackground: Boolean=false,
    
    // 背景颜色（如果有背景）
    val backgroundColor: Int = Color.argb(128, 0, 0, 0),
    
    // 文字对齐方式 (0: 左对齐, 1: 居中, 2: 右对齐)
    val alignment: Int = 1
) : Parcelable {
    
    companion object {
        // 位置常量
        const val POSITION_TOP = 0.2f
        const val POSITION_CENTER = 0.5f
        const val POSITION_BOTTOM = 0.8f
        
        // 默认字体大小（像素）
        const val DEFAULT_FONT_SIZE = 48f
    }
} 