package com.common.taskmanager.listener

import com.common.db.dao.AITaskInfo


/**
 * 任务监听器接口
 */
interface TaskListener {
    /**
     * 任务状态更新回调
     */
    fun onTaskStatusChanged(task: AITaskInfo)
    
    /**
     * 任务进度更新回调
     */
    fun onTaskProgressUpdated(task: AITaskInfo, progress: Int)
}

/**
 * 任务生命周期监听器
 */
interface TaskLifecycleListener {
    /**
     * 任务添加回调
     */
    fun onTaskAdded(task: AITaskInfo)
    
    /**
     * 任务删除回调
     */
    fun onTaskRemoved(tasks: List<AITaskInfo>)
} 