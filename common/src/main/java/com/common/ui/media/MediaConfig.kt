package com.common.ui.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class MediaConfig(val mediaType: Int = MEDIA_TYPE_AUDIO) : Parcelable {







    companion object {
        const val MEDIA_TYPE_IMAGE = 0
        const val MEDIA_TYPE_VIDEO = MEDIA_TYPE_IMAGE + 1
        const val MEDIA_TYPE_AUDIO = MEDIA_TYPE_VIDEO + 1
    }
}
