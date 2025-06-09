package com.common.taskmanager.core

import androidx.annotation.IntDef

/**
 * 任务类型和状态定义
 */
object TaskType {

    /**
     * 任务状态定义
     */
    const val TASK_STATUS_DELETE = -1  // 已删除
    const val TASK_STATUS_CREATE = 0   // 创建
    const val TASK_STATUS_SUCCESS = 1  // 成功
    const val TASK_STATUS_FAILURE = 2  // 失败
    const val TASK_STATUS_RUNNING = 3  // 运行中

    /**
     * 任务状态注解
     */
    @IntDef(
        TASK_STATUS_DELETE,
        TASK_STATUS_CREATE,
        TASK_STATUS_SUCCESS,
        TASK_STATUS_FAILURE,
        TASK_STATUS_RUNNING
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Status
} 