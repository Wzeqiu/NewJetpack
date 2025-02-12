package com.common.kt.mmkv

import android.app.Application
import android.os.Parcelable
import com.tencent.mmkv.MMKV
import kotlinx.parcelize.Parcelize

fun init(application: Application) {
    MMKV.initialize(application)
}

val mmkv by lazy { MMKV.defaultMMKV() }

object MMKVProperty {
    var mmkv_userName by mmkv.initializer("张三")
    var mmkv_age by mmkv.initializer(0)
    var mmkv_heith by mmkv.initializer(20L)
    var mmkv_widht by mmkv.initializer(30.5f)
    var mmkv_grle by mmkv.initializer(false)
    var mmkv_person by mmkv.initializer<Person?>(null)


    @Parcelize
    class Person(val name: String = "", val age: Int = 0) : Parcelable {
        override fun toString(): String {
            return "Person(name='$name', age=$age)"
        }
    }
}


