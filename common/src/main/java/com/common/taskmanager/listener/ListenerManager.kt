package com.common.taskmanager.listener

import com.common.db.dao.AITaskInfo
import com.common.taskmanager.core.TaskType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 监听器管理类，提供更高效的监听器管理和类型过滤功能
 */
class ListenerManager {
    // 使用ConcurrentHashMap和Set提高并发性能，减少CopyOnWriteArrayList在添加/删除时的开销
    private val statusListeners = ConcurrentHashMap<Int, CopyOnWriteArraySet<TaskListener>>()
    private val globalListeners = CopyOnWriteArraySet<TaskListener>()

    /**
     * 添加全局任务监听器(监听所有类型任务)
     */
    fun addTaskListener(listener: TaskListener) {
        globalListeners.add(listener)
    }

    /**
     * 添加特定类型的任务监听器
     */
    fun addTaskListener(listener: TaskListener, @TaskType.Type taskType: Int) {
        statusListeners.getOrPut(taskType) { CopyOnWriteArraySet() }.add(listener)
    }

    /**
     * 移除任务监听器
     */
    fun removeTaskListener(listener: TaskListener) {
        globalListeners.remove(listener)
        // 从所有类型中移除该监听器
        statusListeners.values.forEach { it.remove(listener) }
    }

    /**
     * 通知任务状态变化
     */
    fun notifyTaskStatusChanged(task: AITaskInfo) {
        // 通知特定类型的监听器
        statusListeners[task.type]?.forEach { it.onTaskStatusChanged(task) }
        // 通知全局监听器
        globalListeners.forEach { it.onTaskStatusChanged(task) }
    }

    /**
     * 通知任务添加
     */
    fun notifyTaskAdded(task: AITaskInfo) {
        // 通知特定类型的监听器
        statusListeners[task.type]?.forEach { it.onTaskAdded(task) }
        // 通知全局监听器
        globalListeners.forEach { it.onTaskAdded(task) }
    }

    /**
     * 通知任务删除
     */
    fun notifyTaskRemoved(tasks: List<AITaskInfo>) {
        // 对每个任务通知特定类型的监听器
        tasks.forEach { task ->
            statusListeners[task.type]?.forEach { listener -> 
                listener.onTaskRemoved(listOf(task))
            }
        }
        
        // 通知全局监听器（所有任务一次性通知）
        globalListeners.forEach { it.onTaskRemoved(tasks) }
    }

    /**
     * 清理所有监听器
     */
    fun clear() {
        globalListeners.clear()
        statusListeners.clear()
    }
} 