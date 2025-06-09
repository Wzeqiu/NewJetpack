package com.common.taskmanager.event

/**
 * 任务事件监听器接口
 * 用于监听任务的各种状态变化事件
 */
interface TaskEventListener {
    /**
     * 任务状态变化回调
     * @param event 任务事件
     */
    fun onTaskStatusChanged(event: TaskEvent<*>) {}
    
    /**
     * 任务添加回调
     * @param event 任务事件
     */
    fun onTaskAdded(event: TaskEvent<*>) {}
    
    /**
     * 任务删除回调
     * @param events 任务事件列表
     */
    fun onTaskRemoved(events: List<TaskEvent<*>>) {}
} 