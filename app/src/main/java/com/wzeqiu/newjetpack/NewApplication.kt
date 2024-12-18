package com.wzeqiu.newjetpack

import android.app.Application
import com.common.kt.mmkv.init
import com.hjq.permissions.XXPermissions

class NewApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        init(this)
        XXPermissions.setCheckMode(false)
    }
}