package com.common.taskmanager.executor

import com.common.taskmanager.core.TaskType

/**
 * 图生视频任务执行器
 */
class PictureToVideoExecutor : VideoTaskExecutor() {
    
    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskType.AI_TYPE_PICTURE_TO_VIDEO
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskType.AI_TYPE_PICTURE_TO_VIDEO)
    }
} 