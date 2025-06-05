package com.common.taskmanager.executor

import com.common.db.dao.AITaskInfo
import com.common.taskmanager.core.TaskType
import com.common.taskmanager.listener.TaskListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * 任务执行器接口
 */
abstract class TaskExecutor : CoroutineScope {
    protected val TAG = this::class.java.simpleName
    
    // 使用SupervisorJob确保一个子协程失败不影响其他协程
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()
    
    /**
     * 执行任务
     * 
     * @param task 要执行的任务
     * @param statusListener 状态监听器
     */
    abstract suspend fun executeTask(task: AITaskInfo, statusListener: TaskListener)
    
    /**
     * 取消任务
     * 
     * @param task 要取消的任务
     * @return 是否成功取消
     */
    abstract suspend fun cancelTask(task: AITaskInfo): Boolean
    
    /**
     * 检查任务类型是否被支持
     * 
     * @param taskType 任务类型
     * @return 是否支持
     */
    abstract fun isSupportedTaskType(@TaskType.Type taskType: Int): Boolean
    
    /**
     * 获取执行器支持的任务类型
     * 
     * @return 支持的任务类型列表
     */
    abstract fun getSupportedTaskTypes(): List<Int>
} 