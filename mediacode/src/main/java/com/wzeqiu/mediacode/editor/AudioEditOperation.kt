package com.wzeqiu.mediacode.editor

import com.common.media.MediaInfo

/**
 * 音频编辑操作
 * 
 * 封装音频编辑相关的参数，包括：
 * - 原始音频设置（音量、是否静音）
 * - 背景音乐设置（音乐文件、音量、裁剪范围、是否循环）
 * - 音频效果（渐入渐出）
 */
data class AudioEditOperation(
    // 原始音频音量（0.0-1.0）
    val originalVolume: Float = 1.0f,
    
    // 是否静音原始音频
    val isMuteOriginal: Boolean = false,
    
    // 背景音乐信息
    val backgroundMusic: MediaInfo? = null,
    
    // 背景音乐音量（0.0-1.0）
    val musicVolume: Float = 0.8f,
    
    // 背景音乐裁剪起始时间（毫秒）
    val musicTrimStartMs: Long = 0,
    
    // 背景音乐裁剪结束时间（毫秒）
    val musicTrimEndMs: Long = 0,
    
    // 是否循环播放背景音乐
    val isLoopMusic: Boolean = false,
    
    // 渐入时长（秒）
    val fadeInDurationSec: Float = 0f,
    
    // 渐出时长（秒）
    val fadeOutDurationSec: Float = 0f
) 