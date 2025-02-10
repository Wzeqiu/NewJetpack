package com.common.kt

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners


fun ImageView.load(url: Any) {
    Glide.with(this).load(url).into(this)
}

fun ImageView.loadRound(path: Any, radius: Int = 4f.dp2px) {
    Glide.with(this)
        .load(path)
        .transform(CenterCrop(), RoundedCorners(radius)).into(this)
}