package com.common.media

import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.parcelize.Parcelize

/**
 * 媒体选择配置类
 * 用于配置媒体选择器的行为和过滤条件
 */
@Parcelize
data class MediaConfig(
    /**
     * 媒体类型，用于过滤媒体选择器中显示的内容
     * @see MEDIA_TYPE_IMAGE 图片类型
     * @see MEDIA_TYPE_VIDEO 视频类型
     * @see MEDIA_TYPE_AUDIO 音频类型
     */
    @MediaType
    val mediaType: Int = MEDIA_TYPE_IMAGE,
    
    /**
     * 是否使用原始媒体文件
     * true: 直接使用原始文件路径
     * false: 对媒体文件进行压缩处理后返回压缩后的路径
     */
    val originalMedia: Boolean = true,
    
    /**
     * 是否启用多选模式
     * true: 可以选择多个媒体文件
     * false: 只能选择单个媒体文件
     */
    val enableMultiSelect: Boolean = false,
    
    /**
     * 多选模式下最大可选择的媒体数量
     * 默认为9个，仅在enableMultiSelect为true时有效
     */
    val maxSelectCount: Int = 9
) : Parcelable {
    companion object {
        /** 图片类型 */
        const val MEDIA_TYPE_IMAGE = 0
        
        /** 视频类型 */
        const val MEDIA_TYPE_VIDEO = MEDIA_TYPE_IMAGE + 1
        
        /** 音频类型 */
        const val MEDIA_TYPE_AUDIO = MEDIA_TYPE_VIDEO + 1

        /**
         * 媒体类型注解，用于限制mediaType参数的取值范围
         */
        @IntDef(MEDIA_TYPE_IMAGE, MEDIA_TYPE_VIDEO, MEDIA_TYPE_AUDIO)
        @Retention(AnnotationRetention.SOURCE)
        annotation class MediaType
    }
}
