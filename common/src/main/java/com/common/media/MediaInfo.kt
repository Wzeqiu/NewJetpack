package com.common.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MediaInfo(
    val name: String,
    var path: String,
    val size: Long = 0,
    val duration: Long = 0,
    val mediaType: Int = -1,
    val width: Int = 0,
    val height: Int = 0,
    var isSelect: Boolean = false,
) : Parcelable
