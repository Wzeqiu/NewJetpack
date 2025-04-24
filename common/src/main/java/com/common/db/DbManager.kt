package com.common.db

import android.content.Context
import com.common.db.dao.NewUserDao
import io.objectbox.BoxStore

/**
 * 数据库访问管理器 (单例)
 * 提供各个 DAO 的实例
 */
object DbManager {
    lateinit var store: BoxStore
    fun init(context: Context) {
        store = MyObjectBox.builder()
            .androidContext(context)
            .build()
    }
    /**
     * 获取 NewUserDao 实例
     */
    val newUserDao: NewUserDao by lazy { NewUserDao() }

}