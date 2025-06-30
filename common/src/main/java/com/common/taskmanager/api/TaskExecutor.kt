package com.common.taskmanager.api

/**
 * 任务执行器接口
 * 用于执行和取消各种类型的任务
 * @param T 任务对象类型
 */
interface TaskExecutor<T : Any> {

    /**
     * 设置任务状态回调
     */
    fun setCallBack(callback: TaskCallback<Any>)

    /**
     * 设置任务适配器
     */
    fun setAdapter(adapter: TaskAdapter<T>)

    /**
     * 执行任务
     * @param task 任务对象
     */
    suspend fun execute(task: T)

    /**
     * 取消任务
     * @param task 任务对象
     * @return 是否成功取消
     */
    suspend fun cancel(task: T): Boolean

    /**
     * 取消全部任务
     * @return 是否成功取消
     */
    suspend fun cancelAll(): Boolean

    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(task: T)

    /**
     * 获取任务类类型
     * 默认实现，子类可以覆盖
     */
    fun getTaskClass(): Class<T>
}