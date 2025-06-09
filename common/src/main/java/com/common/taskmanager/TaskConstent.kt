package com.common.taskmanager

import androidx.annotation.IntDef
import com.common.taskmanager.core.TaskType

object TaskConstent{
    /**
     * 任务类型定义
     */
    const val AI_TYPE_ORAL_BROADCASTING = 18  // AI口播
    const val AI_TYPE_PICTURE_TO_VIDEO = 19   // 图生视频
    const val AI_TYPE_TEXT_TO_IMAGE = 20      // 文生图



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
}

