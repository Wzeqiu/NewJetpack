package com.common.taskmanager.api

/**
 * 任务事件类
 * 封装任务对象和适配器，提供统一的任务信息访问接口
 * @param T 任务对象类型
 * @property task 任务对象
 * @property adapter 任务适配器
 */
class TaskEvent<T>(
    val task: T,
    val adapter: TaskAdapter<T>
) {
    /**
     * 获取任务ID
     */
    fun getTaskId(): String = adapter.getTaskId(task)

    /**
     * 获取任务类型
     */
    fun getType(): Int = adapter.getType(task)

    /**
     * 获取任务状态
     */
    fun getStatus(): Int = adapter.getStatus(task)

    /**
     * 获取任务实体类型
     */
    fun getTaskClass(): Class<T> = adapter.getTaskClass()

    /**
     * 判断任务是否为活跃状态
     */
    fun isActive(): Boolean = adapter.isActive(task)

    /**
     * 判断任务是否已完成
     */
    fun isCompleted(): Boolean = adapter.isCompleted(task)

    /**
     * 判断任务是否已失败
     */
    fun isFailed(): Boolean = adapter.isFailed(task)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaskEvent<*>) return false

        return getTaskId() == other.getTaskId() && 
               getTaskClass() == other.getTaskClass()
    }

    override fun hashCode(): Int {
        var result = getTaskId().hashCode()
        result = 31 * result + getTaskClass().hashCode()
        return result
    }

    override fun toString(): String {
        return "TaskEvent(taskId=${getTaskId()}, type=${getType()}, status=${getStatus()})"
    }
} 