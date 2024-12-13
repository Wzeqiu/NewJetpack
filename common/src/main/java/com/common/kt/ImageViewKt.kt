package com.common.kt

import android.widget.ImageView
import com.bumptech.glide.Glide


fun ImageView.load(url: Any) {
    Glide.with(this).load(url).into(this)
}