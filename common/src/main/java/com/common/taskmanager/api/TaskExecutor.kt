package com.mxm.douying.aigc.taskmanager.api

import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback

/**
 * 任务执行器接口
 * 用于执行和取消各种类型的任务
 * @param T 任务对象类型
 */
interface TaskExecutor {


    /**
     * 设置任务状态回调
     */
    fun setCallBack(callback: TaskCallback<Any>)


    /**
     *
     */
    fun setAdapter(adapter: TaskAdapter<*>)

    /**
     * 执行任务
     * @param task 任务对象
     * @param adapter 任务适配器
     * @param callback 任务状态回调
     */
    suspend fun execute(task: Any)


    /**
     * 取消任务
     * @param task 任务对象
     * @param adapter 任务适配器
     * @return 是否成功取消
     */
    suspend fun cancel(task: Any): Boolean

    /**
     * 取消全部任务
     * @return 是否成功取消
     */
    suspend fun cancelAll(): Boolean


    /**
     * 更新任务状态
     */
    suspend fun updateTaskStatus(task: Any)


    /**
     * 获取任务类类型
     * 默认实现，子类可以覆盖
     */
    fun getTaskClass(): Class<*>
}