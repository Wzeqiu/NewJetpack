package com.common.kt

import android.content.res.Resources
import android.util.TypedValue


inline val Number.dp2px: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, this.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }

inline val Number.sp2px: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, this.toFloat(), Resources.getSystem().displayMetrics
        ).toInt()
    }


