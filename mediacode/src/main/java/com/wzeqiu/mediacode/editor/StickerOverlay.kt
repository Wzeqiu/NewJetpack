package com.wzeqiu.mediacode.editor

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


/**
 * 贴纸覆盖层数据模型
 * 
 * 用于保存和管理贴纸在视频中的属性信息
 */
@Parcelize
data class StickerOverlay(
    // 贴纸资源ID
    val resourceId: Int,
    
    // 贴纸类别
    val category: String,
    
    // 贴纸在视频中的显示时间范围（毫秒）
    val startTimeMs: Long,
    val endTimeMs: Long,
    
    // 贴纸在视频中的位置（0.0-1.0表示相对位置）
    val xPosition: Float,
    val yPosition: Float,
    
    // 贴纸大小（相对于视频的大小比例，0.0-1.0）
    val size: Float,
    
    // 贴纸旋转角度（0-360度）
    val rotation: Float,
    
    // 贴纸透明度（0-255）
    val alpha: Int
) : Parcelable {

    companion object {
        // 贴纸类别常量
        const val CATEGORY_EMOJI = "emoji"
        const val CATEGORY_DECORATION = "decoration"
        const val CATEGORY_EFFECT = "effect"
        
        // 默认值
        const val DEFAULT_SIZE = 0.2f
        const val DEFAULT_ROTATION = 0f
        const val DEFAULT_ALPHA = 255
    }
    
    /**
     * 创建新的贴纸覆盖层，并应用新的属性值
     */
    fun copyWith(
        resourceId: Int = this.resourceId,
        category: String = this.category,
        startTimeMs: Long = this.startTimeMs,
        endTimeMs: Long = this.endTimeMs,
        xPosition: Float = this.xPosition,
        yPosition: Float = this.yPosition,
        size: Float = this.size,
        rotation: Float = this.rotation,
        alpha: Int = this.alpha
    ): StickerOverlay {
        return StickerOverlay(
            resourceId = resourceId,
            category = category,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            xPosition = xPosition,
            yPosition = yPosition,
            size = size,
            rotation = rotation,
            alpha = alpha
        )
    }
}

/**
 * 贴纸数据
 * 
 * 表示一个可用的贴纸
 */
data class StickerData(
    // 贴纸资源ID
    val resourceId: Int,
    
    // 贴纸名称
    val name: String,
    
    // 贴纸类别
    val category: String
) 