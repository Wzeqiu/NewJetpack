package com.common.taskmanager.core

import androidx.annotation.IntDef

/**
 * 任务类型和状态定义
 */
object TaskType {
    /**
     * 任务类型定义
     */
    const val AI_TYPE_ORAL_BROADCASTING = 18  // AI口播
    const val AI_TYPE_PICTURE_TO_VIDEO = 19   // 图生视频
    const val AI_TYPE_TEXT_TO_IMAGE = 20      // 文生图

    /**
     * 任务状态定义
     */
    const val TASK_STATUS_DELETE = -1  // 已删除
    const val TASK_STATUS_CREATE = 0   // 创建
    const val TASK_STATUS_SUCCESS = 1  // 成功
    const val TASK_STATUS_FAILURE = 2  // 失败

    /**
     * 任务类型注解
     */
    @IntDef(
        AI_TYPE_ORAL_BROADCASTING,
        AI_TYPE_PICTURE_TO_VIDEO,
        AI_TYPE_TEXT_TO_IMAGE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type

    /**
     * 任务状态注解
     */
    @IntDef(TASK_STATUS_DELETE, TASK_STATUS_CREATE, TASK_STATUS_SUCCESS, TASK_STATUS_FAILURE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Status
} 