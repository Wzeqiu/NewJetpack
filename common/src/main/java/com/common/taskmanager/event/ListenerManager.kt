package com.common.taskmanager.event

import com.common.taskmanager.TaskConstent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * 监听器管理类
 * 用于管理任务事件监听器的注册和通知
 */
class ListenerManager {
    // 类型监听器映射，按任务类型分类
    private val typeListeners = ConcurrentHashMap<Int, CopyOnWriteArraySet<TaskEventListener>>()
    
    // 全局监听器，监听所有任务事件
    private val globalListeners = CopyOnWriteArraySet<TaskEventListener>()
    
    /**
     * 添加全局任务监听器
     */
    fun addListener(listener: TaskEventListener) {
        globalListeners.add(listener)
    }
    
    /**
     * 添加特定类型的任务监听器
     */
    fun addListener(listener: TaskEventListener, @TaskConstent.Type taskType: Int) {
        val listeners = typeListeners.getOrPut(taskType) { CopyOnWriteArraySet() }
        listeners.add(listener)
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
    }
    
    /**
     * 通知任务状态变化
     */
    fun notifyTaskStatusChanged(event: TaskEvent<*>) {
        // 通知特定类型的监听器
        typeListeners[event.getType()]?.forEach { it.onTaskStatusChanged(event) }
        
        // 通知全局监听器
        globalListeners.forEach { it.onTaskStatusChanged(event) }
    }
    
    /**
     * 通知任务添加
     */
    fun notifyTaskAdded(event: TaskEvent<*>) {
        // 通知特定类型的监听器
        typeListeners[event.getType()]?.forEach { it.onTaskAdded(event) }
        
        // 通知全局监听器
        globalListeners.forEach { it.onTaskAdded(event) }
    }
    
    /**
     * 通知任务删除
     */
    fun notifyTasksRemoved(events: List<TaskEvent<*>>) {
        if (events.isEmpty()) return
        
        // 按任务类型分组
        val eventsByType = events.groupBy { it.getType() }
        
        // 对每种类型的任务通知对应的监听器
        eventsByType.forEach { (type, typeEvents) ->
            typeListeners[type]?.forEach { listener ->
                listener.onTaskRemoved(typeEvents)
            }
        }
        
        // 通知全局监听器（所有任务一次性通知）
        globalListeners.forEach { it.onTaskRemoved(events) }
    }
    
    /**
     * 清空所有监听器
     */
    fun clear() {
        typeListeners.clear()
        globalListeners.clear()
    }
} 