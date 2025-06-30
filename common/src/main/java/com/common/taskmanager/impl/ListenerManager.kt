package com.common.taskmanager.impl

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskEvent
import com.common.taskmanager.api.TaskEventListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 监听器管理类
 * 用于管理任务事件监听器的注册和通知
 */
class ListenerManager {
    companion object {
        private const val TAG = "ListenerManager"
    }

    // 类型监听器映射，按任务类型分类
    private val typeListeners = ConcurrentHashMap<Int, CopyOnWriteArraySet<TaskEventListener>>()
    
    // 全局监听器，监听所有任务事件
    private val globalListeners = CopyOnWriteArraySet<TaskEventListener>()
    
    /**
     * 添加全局任务监听器
     */
    fun addListener(listener: TaskEventListener) {
        globalListeners.add(listener)
        LogUtils.d(TAG, "添加全局监听器: ${listener.javaClass.simpleName}")
    }
    
    /**
     * 添加特定类型的任务监听器
     */
    fun addListener(listener: TaskEventListener, @TaskConstant.Type taskType: Int) {
        val listeners = typeListeners.getOrPut(taskType) { CopyOnWriteArraySet() }
        listeners.add(listener)
        LogUtils.d(TAG, "添加类型 $taskType 监听器: ${listener.javaClass.simpleName}")
    }
    
    /**
     * 移除任务监听器
     */
    fun removeListener(listener: TaskEventListener) {
        globalListeners.remove(listener)
        
        // 从所有类型监听器中移除
        typeListeners.values.forEach { it.remove(listener) }
        
        // 清理空的监听器集合
        typeListeners.entries.removeIf { it.value.isEmpty() }
        
        LogUtils.d(TAG, "移除监听器: ${listener.javaClass.simpleName}")
    }
    
    /**
     * 通知任务状态变化
     * 包含异常处理，确保一个监听器的异常不会影响其他监听器
     */
    fun notifyTaskStatusChanged(event: TaskEvent<*>) {
        // 通知特定类型的监听器
        typeListeners[event.getType()]?.forEach { listener ->
            try {
                listener.onTaskStatusChanged(event)
            } catch (e: Exception) {
                LogUtils.e(TAG, "通知任务状态变化异常: ${listener.javaClass.simpleName}", e)
            }
        }
        
        // 通知全局监听器
        globalListeners.forEach { listener ->
            try {
                listener.onTaskStatusChanged(event)
            } catch (e: Exception) {
                LogUtils.e(TAG, "通知任务状态变化异常: ${listener.javaClass.simpleName}", e)
            }
        }
    }
    
    /**
     * 通知任务添加
     * 包含异常处理，确保一个监听器的异常不会影响其他监听器
     */
    fun notifyTaskAdded(event: TaskEvent<*>) {
        // 通知特定类型的监听器
        typeListeners[event.getType()]?.forEach { listener ->
            try {
                listener.onTaskAdded(event)
            } catch (e: Exception) {
                LogUtils.e(TAG, "通知任务添加异常: ${listener.javaClass.simpleName}", e)
            }
        }
        
        // 通知全局监听器
        globalListeners.forEach { listener ->
            try {
                listener.onTaskAdded(event)
            } catch (e: Exception) {
                LogUtils.e(TAG, "通知任务添加异常: ${listener.javaClass.simpleName}", e)
            }
        }
    }
    
    /**
     * 通知任务删除
     * 包含异常处理，确保一个监听器的异常不会影响其他监听器
     */
    fun notifyTasksRemoved(events: List<TaskEvent<*>>) {
        if (events.isEmpty()) return
        
        // 按任务类型分组
        val eventsByType = events.groupBy { it.getType() }
        
        // 对每种类型的任务通知对应的监听器
        eventsByType.forEach { (type, typeEvents) ->
            typeListeners[type]?.forEach { listener ->
                try {
                    listener.onTaskRemoved(typeEvents)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "通知任务删除异常: ${listener.javaClass.simpleName}", e)
                }
            }
        }
        
        // 通知全局监听器（所有任务一次性通知）
        globalListeners.forEach { listener ->
            try {
                listener.onTaskRemoved(events)
            } catch (e: Exception) {
                LogUtils.e(TAG, "通知任务删除异常: ${listener.javaClass.simpleName}", e)
            }
        }
    }
    
    /**
     * 清空所有监听器
     */
    fun clear() {
        typeListeners.clear()
        globalListeners.clear()
        LogUtils.d(TAG, "清空所有监听器")
    }
    
    /**
     * 获取所有监听器数量
     * @return 监听器数量
     */
    fun getListenerCount(): Int {
        return globalListeners.size + typeListeners.values.sumOf { it.size }
    }
} 