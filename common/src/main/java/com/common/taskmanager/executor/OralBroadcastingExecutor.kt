package com.common.taskmanager.executor

import com.common.taskmanager.core.TaskType

/**
 * AI口播任务执行器
 */
class OralBroadcastingExecutor : VideoTaskExecutor() {
    
    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskType.AI_TYPE_ORAL_BROADCASTING
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskType.AI_TYPE_ORAL_BROADCASTING)
    }
} 