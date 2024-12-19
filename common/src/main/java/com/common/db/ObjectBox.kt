package com.common.db

import android.content.Context
import com.common.MyObjectBox
import io.objectbox.BoxStore

object ObjectBox {
    private lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        store = MyObjectBox.builder()
            .androidContext(context)
            .build()
    }
}