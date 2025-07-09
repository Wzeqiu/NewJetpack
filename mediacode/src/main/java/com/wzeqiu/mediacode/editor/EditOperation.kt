package com.wzeqiu.mediacode.editor

/**
 * 编辑操作类
 * 
 * 用于记录各种编辑操作，支持撤销/重做功能
 */
sealed class EditOperation {
    /**
     * 裁剪操作
     */
    data class Trim(
        val startMs: Long,
        val endMs: Long
    ) : EditOperation()
    
    /**
     * 变速操作
     */
    data class Speed(
        val speed: Float
    ) : EditOperation()
    
    /**
     * 旋转操作
     */
    data class Rotation(
        val degrees: Float
    ) : EditOperation()
    
    /**
     * 翻转操作
     */
    data class Flip(
        val horizontal: Boolean,
        val vertical: Boolean
    ) : EditOperation()
    
    /**
     * 音量操作
     */
    data class Volume(
        val volume: Float
    ) : EditOperation()
    
    /**
     * 滤镜操作
     */
    data class Filter(
        val filter: VideoFilter
    ) : EditOperation()
    
    /**
     * 音频操作
     */
    data class Audio(
        val audioOperation: AudioEditOperation
    ) : EditOperation()
    
    /**
     * 文本/字幕操作
     */
    data class Text(
        val text: String,
        val startTimeMs: Long,
        val endTimeMs: Long,
        val x: Float,
        val y: Float,
        val fontSize: Float,
        val color: Int
    ) : EditOperation()
} 