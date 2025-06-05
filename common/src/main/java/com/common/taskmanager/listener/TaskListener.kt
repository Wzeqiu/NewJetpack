package com.common.taskmanager.listener

import com.common.db.dao.AITaskInfo

/**
 * 任务监听器接口
 * 所有方法均为可选实现
 */
interface TaskListener {
    /**
     * 任务状态更新回调
     */
    fun onTaskStatusChanged(task: AITaskInfo) {}
    
    /**
     * 任务添加回调
     */
    fun onTaskAdded(task: AITaskInfo) {}
    
    /**
     * 任务删除回调
     */
    fun onTaskRemoved(tasks: List<AITaskInfo>) {}
} 