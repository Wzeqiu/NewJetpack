package com.common.taskmanager.api

/**
 * 任务回调接口
 * 用于任务执行过程中的状态更新回调
 * @param T 任务对象类型
 */
interface TaskCallback<T> {
    /**
     * 任务状态变化回调
     * @param task 任务对象
     */
    fun onStatusChanged(task: T)
    
    /**
     * 任务进度更新回调
     * @param task 任务对象
     * @param progress 进度(0-100)
     */
    fun onProgressUpdate(task: T, progress: Int) {
        onStatusChanged(task)
    }
    
    /**
     * 任务成功完成回调
     * @param task 任务对象
     * @param result 结果数据
     */
    fun onSuccess(task: T, result: String?) {
        onStatusChanged(task)
    }
    
    /**
     * 任务失败回调
     * @param task 任务对象
     * @param reason 失败原因
     */
    fun onFailure(task: T, reason: String?) {
        onStatusChanged(task)
    }
} 