package com.common.taskmanager.api

import com.common.taskmanager.TaskConstant

/**
 * 任务执行器接口
 * 用于执行和取消各种类型的任务
 * @param T 任务对象类型
 * @param A 任务适配器类型
 */
interface TaskExecutor {

    /**
     * 执行任务
     * @param task 任务对象
     * @param adapter 任务适配器
     * @param callback 任务状态回调
     */
    suspend fun execute(task: Any, adapter: TaskAdapter<*>, callback: TaskCallback<*>)


    /**
     * 取消任务
     * @param task 任务对象
     * @param adapter 任务适配器
     * @return 是否成功取消
     */
    suspend fun cancel(task: Any, adapter: TaskAdapter<*>): Boolean


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
}