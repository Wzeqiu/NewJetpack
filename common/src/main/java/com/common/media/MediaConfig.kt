package com.common.media

import android.os.Parcelable
import androidx.annotation.IntDef
import kotlinx.parcelize.Parcelize


@Parcelize
data class MediaConfig(
    @MediaType
    val mediaType: Int = MEDIA_TYPE_IMAGE,
    val originalMedia: Boolean = true
) : Parcelable {
    companion object {
        const val MEDIA_TYPE_IMAGE = 0
        const val MEDIA_TYPE_VIDEO = MEDIA_TYPE_IMAGE + 1
        const val MEDIA_TYPE_AUDIO = MEDIA_TYPE_VIDEO + 1

        @IntDef(MEDIA_TYPE_IMAGE, MEDIA_TYPE_VIDEO, MEDIA_TYPE_AUDIO)
        @Retention(AnnotationRetention.SOURCE)
        annotation class MediaType
    }
}
