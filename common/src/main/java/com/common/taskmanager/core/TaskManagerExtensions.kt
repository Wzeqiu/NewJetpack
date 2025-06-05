package com.common.taskmanager.core

import com.common.db.dao.AITaskInfo

/**
 * TaskManager的扩展函数
 */

/**
 * 检查是否存在运行中的口播任务
 */
fun TaskManager.hasRunningOralBroadcastingTask(): Boolean {
    return getTasks().any { 
        it.isCreate() && it.type == TaskType.AI_TYPE_ORAL_BROADCASTING
    }
}

/**
 * 检查是否存在运行中的图生视频任务
 */
fun TaskManager.hasRunningPictureToVideoTask(): Boolean {
    return getTasks().any { 
        it.isCreate() && it.type == TaskType.AI_TYPE_PICTURE_TO_VIDEO
    }
}

/**
 * 检查是否存在运行中的文生图任务
 */
fun TaskManager.hasRunningTextToImageTask(): Boolean {
    return getTasks().any { 
        it.isCreate() && it.type == TaskType.AI_TYPE_TEXT_TO_IMAGE
    }
}

/**
 * 获取所有正在运行的任务
 */
fun TaskManager.getRunningTasks(): List<AITaskInfo> {
    return getTasks().filter { it.isCreate() }
}

/**
 * 获取指定类型的运行中任务
 */
fun TaskManager.getRunningTasksByType(taskType: Int): List<AITaskInfo> {
    return getTasks().filter { it.isCreate() && it.type == taskType }
}

/**
 * 获取所有成功的任务
 */
fun TaskManager.getSuccessTasks(): List<AITaskInfo> {
    return getTasks().filter { it.isSuccess() }
}

/**
 * 获取所有失败的任务
 */
fun TaskManager.getFailureTasks(): List<AITaskInfo> {
    return getTasks().filter { it.isFailure() }
} 