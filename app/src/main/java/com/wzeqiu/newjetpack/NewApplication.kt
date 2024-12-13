package com.wzeqiu.newjetpack

import android.app.Application
import com.common.kt.mmkv.init

class NewApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        init(this)
    }
}