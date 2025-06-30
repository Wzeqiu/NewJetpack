package com.wzeqiu.newjetpack

import android.app.Application
import com.common.db.dao.AITaskInfo
import com.common.kt.mmkv.init
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.core.TaskManager
import com.common.taskmanager.ext.AITaskInfoAdapter
import com.common.taskmanager.ext.TextToImageExecutor
import com.hjq.permissions.XXPermissions

class NewApplication :Application() {
    override fun onCreate() {
        super.onCreate()
        init(this)
        XXPermissions.setCheckMode(false)
        TaskManager.getInstance().registerAdapterClass(AITaskInfo::class.java, AITaskInfoAdapter::class.java)
        TaskManager.getInstance().registerExecutorClass(TaskConstant.AI_TYPE_TEXT_TO_IMAGE, TextToImageExecutor::class.java)
    }
}