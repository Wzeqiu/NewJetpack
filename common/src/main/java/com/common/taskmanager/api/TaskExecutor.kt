package com.common.taskmanager.api

import com.common.taskmanager.TaskConstant

/**
 * 任务执行器接口
 * 用于执行和取消各种类型的任务
 * @param T 任务对象类型
 * @param A 任务适配器类型
 */
interface TaskExecutor<T : Any, A : TaskAdapter<T>> {


    /**
     * 任务适配器
     */
    val adapter: A

    /**
     * 执行任务
     * @param task 任务对象
     * @param adapter 任务适配器
     * @param callback 任务状态回调
     */
    suspend fun <U> execute(task: U, adapter: TaskAdapter<U>, callback: TaskCallback<U>)

    /**
     * 执行强类型任务
     * @param task 特定类型的任务对象
     * @param callback 任务状态回调
     */
    suspend fun executeTyped(task: T, callback: TaskCallback<T>)

    /**
     * 取消任务
     * @param task 任务对象
     * @param adapter 任务适配器
     * @return 是否成功取消
     */
    suspend fun <U> cancel(task: U, adapter: TaskAdapter<U>): Boolean

    /**
     * 取消强类型任务
     * @param task 特定类型的任务对象
     * @return 是否成功取消
     */
    suspend fun cancelTyped(task: T): Boolean

    /**
     * 检查任务类型是否被支持
     * @param taskType 任务类型
     * @return 是否支持
     */
    fun isSupportedTaskType(@TaskConstant.Type taskType: Int): Boolean

    /**
     * 获取执行器支持的任务类型
     * @return 支持的任务类型列表
     */
    fun getSupportedTaskTypes(): List<Int>


    /**
     * 获取任务类类型
     * @return 任务对象的Class
     */
    fun getTaskClass(): Class<T>

}