package com.common.taskmanager.impl

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.api.TaskExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * 抽象任务执行器
 * 提供任务执行器的基础实现
 */
abstract class AbstractTaskExecutor : TaskExecutor, CoroutineScope {
    protected val TAG = this::class.java.simpleName

    // 使用SupervisorJob确保一个子协程失败不影响其他协程
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    // 互斥锁，用于保护共享状态
    private val mutex = Mutex()
    
    // 运行中的任务列表，按任务ID索引
    private val runningJobs = ConcurrentHashMap<String, Job>()

    /**
     * 执行任务
     * 通用实现，处理基本的任务生命周期
     */
    override suspend fun <T> execute(task: T, adapter: TaskAdapter<T>, callback: TaskCallback<T>) {
        val taskId = adapter.getTaskId(task)
        
        // 取消已存在的相同ID任务
        cancel(task, adapter)
        
        mutex.withLock {
            // 创建并启动任务作业
            val job = launch {
                try {
                    LogUtils.d(TAG, "开始执行任务: $taskId, 类型: ${adapter.getType(task)}")
                    
                    // 标记任务开始执行
                    adapter.markStarted(task)
                    callback.onStatusChanged(task)
                    
                    // 执行具体的任务处理逻辑
                    doExecute(task, adapter, callback)
                    
                    LogUtils.d(TAG, "任务执行完成: $taskId")
                } catch (e: CancellationException) {
                    LogUtils.w(TAG, "任务已取消: $taskId")
                    throw e
                } catch (e: Exception) {
                    LogUtils.e(TAG, "任务执行异常: $taskId", e)
                    adapter.markFailure(task, e.message)
                    callback.onFailure(task, e.message)
                } finally {
                    // 移除运行中的任务记录
                    runningJobs.remove(taskId)
                }
            }
            
            runningJobs[taskId] = job
        }
    }

    /**
     * 执行具体的任务处理逻辑，由子类实现
     */
    protected abstract suspend fun <T> doExecute(
        task: T,
        adapter: TaskAdapter<T>,
        callback: TaskCallback<T>
    )

    /**
     * 取消任务
     */
    override suspend fun <T> cancel(task: T, adapter: TaskAdapter<T>): Boolean {
        val taskId = adapter.getTaskId(task)
        return mutex.withLock {
            val job = runningJobs[taskId]
            if (job != null && job.isActive) {
                job.cancel()
                runningJobs.remove(taskId)
                LogUtils.d(TAG, "任务已取消: $taskId")
                true
            } else {
                LogUtils.w(TAG, "任务不存在或已完成，无法取消: $taskId")
                false
            }
        }
    }

    /**
     * 判断任务是否在执行
     */
    protected fun isTaskRunning(taskId: String): Boolean {
        return runningJobs[taskId]?.isActive == true
    }
} 