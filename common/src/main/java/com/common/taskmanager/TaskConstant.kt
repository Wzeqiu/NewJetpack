package com.common.taskmanager

import androidx.annotation.IntDef

/**
 * 任务相关常量定义
 */
object TaskConstant {
    /**
     * 任务类型定义
     */
    const val AI_TYPE_ORAL_BROADCASTING = 18  // AI口播
    const val AI_TYPE_PICTURE_TO_VIDEO = 19   // 图生视频
    const val AI_TYPE_TEXT_TO_IMAGE = 20      // 文生图
    const val AI_TYPE_VIDEO_GENERATION = 21   // 视频生成
    const val AI_TYPE_VIDEO_EDITING = 22      // 视频编辑

    /**
     * 任务类型注解
     */
    @IntDef(
        AI_TYPE_ORAL_BROADCASTING,
        AI_TYPE_PICTURE_TO_VIDEO,
        AI_TYPE_TEXT_TO_IMAGE,
        AI_TYPE_VIDEO_GENERATION,
        AI_TYPE_VIDEO_EDITING
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class Type

    /**
     * 任务状态定义
     */
    const val TASK_STATUS_DELETE = -1  // 已删除
    const val TASK_STATUS_CREATE = 0   // 创建
    const val TASK_STATUS_RUNNING = 1  // 运行中
    const val TASK_STATUS_SUCCESS = 2  // 成功
    const val TASK_STATUS_FAILURE = 3  // 失败

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

