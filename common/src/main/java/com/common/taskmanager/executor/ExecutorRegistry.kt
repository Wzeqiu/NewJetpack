package com.common.taskmanager.executor

import com.blankj.utilcode.util.LogUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * 执行器注册表，负责管理任务执行器的注册和查找
 */
class ExecutorRegistry {
    companion object {
        private const val TAG = "ExecutorRegistry"
    }

    // 任务类型到执行器的映射
    private val executors = ConcurrentHashMap<Int, TaskExecutor>()

    /**
     * 注册任务执行器
     * 
     * @param executor 任务执行器
     * @return 是否注册成功
     */
    fun registerExecutor(executor: TaskExecutor): Boolean {
        val supportedTypes = executor.getSupportedTaskTypes()
        if (supportedTypes.isEmpty()) {
            LogUtils.w(TAG, "执行器不支持任何任务类型: ${executor.javaClass.simpleName}")
            return false
        }

        var registered = false
        supportedTypes.forEach { type ->
            if (executors.containsKey(type)) {
                val existing = executors[type]
                LogUtils.w(TAG, "任务类型 $type 已被 ${existing?.javaClass?.simpleName} 注册，将被 ${executor.javaClass.simpleName} 覆盖")
            }
            executors[type] = executor
            registered = true
        }

        if (registered) {
            LogUtils.d(TAG, "执行器注册成功: ${executor.javaClass.simpleName}，支持类型: $supportedTypes")
        }
        return registered
    }

    /**
     * 注销任务执行器
     * 
     * @param executor 任务执行器
     */
    fun unregisterExecutor(executor: TaskExecutor) {
        val typesToRemove = executors.entries
            .filter { it.value == executor }
            .map { it.key }
            .toList()

        typesToRemove.forEach { type ->
            executors.remove(type)
        }

        if (typesToRemove.isNotEmpty()) {
            LogUtils.d(TAG, "执行器注销成功: ${executor.javaClass.simpleName}，类型: $typesToRemove")
        }
    }

    /**
     * 获取指定类型的任务执行器
     * 
     * @param taskType 任务类型
     * @return 对应的执行器，若不存在则返回null
     */
    fun getExecutor(taskType: Int): TaskExecutor? {
        return executors[taskType]
    }

    /**
     * 获取所有已注册的执行器
     */
    fun getAllExecutors(): Collection<TaskExecutor> {
        return executors.values.toSet()
    }

    /**
     * 检查是否有执行器支持指定类型的任务
     */
    fun isTaskTypeSupported(taskType: Int): Boolean {
        return executors.containsKey(taskType)
    }

    /**
     * 清空所有执行器
     */
    fun clear() {
        executors.clear()
    }
} 