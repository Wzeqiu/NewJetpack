package com.common.taskmanager.adapter

import com.common.taskmanager.core.TaskType

/**
 * 任务适配器接口
 * 用于将不同表的任务对象适配为统一的操作接口
 * @param T 任务对象类型
 */
interface TaskAdapter<T> {
    /**
     * 获取任务ID
     */
    fun getTaskId(task: T): String

    /**
     * 获取任务类型
     */
    fun getType(task: T): Int

    /**
     * 获取任务状态
     */
    fun getStatus(task: T): Int

    /**
     * 持久化保存任务
     */
    suspend fun saveTask(task: T)

    /**
     * 更新任务
     */
    suspend fun updateTask(task: T)

    /**
     * 删除任务
     */
    suspend fun deleteTask(task: T)

    /**
     * 加载所有任务
     */
    suspend fun loadAllTasks(): List<T>

    /**
     * 查找特定ID的任务
     */
    suspend fun findTask(taskId: String): T?

    /**
     * 设置任务状态
     */
    fun setStatus(task: T, @TaskType.Status status: Int)

    /**
     * 设置任务结果
     */
    fun setResult(task: T, result: String?)

    /**
     * 设置任务进度
     */
    fun setProgress(task: T, progress: Int)

    /**
     * 标记任务开始执行
     */
    fun markStarted(task: T)

    /**
     * 标记任务成功完成
     */
    fun markSuccess(task: T, result: String?)

    /**
     * 标记任务失败
     */
    fun markFailure(task: T, reason: String?)

    /**
     * 判断任务是否为活跃状态（非终止状态）
     */
    fun isActive(task: T): Boolean {
        val status = getStatus(task)
        return status == TaskType.TASK_STATUS_CREATE || 
               status == TaskType.TASK_STATUS_RUNNING
    }

    /**
     * 判断任务是否已完成
     */
    fun isCompleted(task: T): Boolean {
        return getStatus(task) == TaskType.TASK_STATUS_SUCCESS
    }

    /**
     * 判断任务是否已失败
     */
    fun isFailed(task: T): Boolean {
        return getStatus(task) == TaskType.TASK_STATUS_FAILURE
    }

    /**
     * 获取任务实体类类型
     */
    fun getTaskClass(): Class<T>
} 