package com.common.taskmanager.impl

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.api.TaskExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

/**
 * 抽象任务执行器
 * 提供任务执行器的基础实现
 * @param T 任务对象类型
 */
abstract class AbstractTaskExecutor<T : Any> : TaskExecutor<T>,
    CoroutineScope {
    protected val TAG = this::class.java.simpleName

    // 使用SupervisorJob确保一个子协程失败不影响其他协程
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    // 互斥锁，用于保护共享状态
    private val mutex = Mutex()

    // 运行中的任务列表，按任务ID索引
    private val runningJobs = ArrayList<String>()

    protected lateinit var taskCallback: TaskCallback<Any>
    protected lateinit var taskAdapter: TaskAdapter<T>

    override fun setCallBack(callback: TaskCallback<Any>) {
        this.taskCallback = callback
    }

    override fun setAdapter(adapter: TaskAdapter<T>) {
        this.taskAdapter = adapter
    }

    /**
     * 执行任务
     */
    override suspend fun execute(task: T) {
        val taskId = taskAdapter.getTaskId(task)
        // 已存在的相同ID任务
        if (runningJobs.contains(taskId)) {
            LogUtils.w(TAG, "任务已存在: $taskId")
            return
        }

        mutex.withLock {
            launch {
                try {
                    LogUtils.d(TAG, "开始执行任务: $taskId, 类型: ${taskAdapter.getType(task)}")
                    doExecute(task)
                    // 标记任务开始执行
                    taskAdapter.markStarted(task)
                    taskCallback.onStatusChanged(task)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "任务执行异常: $taskId", e)
                    taskAdapter.markFailure(task)
                    taskCallback.onStatusChanged(task)
                    mutex.withLock {
                        runningJobs.remove(taskId)
                    }
                }
            }
            runningJobs.add(taskId)
        }
    }

    /**
     * 取消任务
     */
    override suspend fun cancel(task: T): Boolean {
        val taskId = taskAdapter.getTaskId(task)
        mutex.withLock {
            if (!runningJobs.contains(taskId)) {
                // 任务不在运行状态
                return false
            }
            // 移除运行中任务
            runningJobs.remove(taskId)
            LogUtils.d(TAG, "取消任务: $taskId")
            return true
        }
    }

    /**
     * 取消所有任务
     */
    override suspend fun cancelAll(): Boolean {
        mutex.withLock {
            if (runningJobs.isEmpty()) {
                return false
            }
            // 清空所有运行中任务
            runningJobs.clear()
            LogUtils.d(TAG, "取消所有任务")
            return true
        }
    }

    /**
     * 任务成功
     */
    suspend fun upDataSuccess(task: T) {
        cancel(task)
        taskAdapter.markSuccess(task)
        updateTaskStatus(task)
    }

    /**
     * 任务失败
     */
    suspend fun upDataFailure(task: T) {
        cancel(task)
        taskAdapter.markFailure(task)
        updateTaskStatus(task)
    }

    /**
     * 更新任务状态
     */
    override suspend fun updateTaskStatus(task: T) {
        val taskId = taskAdapter.getTaskId(task)
        val status = taskAdapter.getStatus(task)
        LogUtils.d(TAG, "更新任务状态: $taskId, 状态: $status")
        taskCallback.onStatusChanged(task)
    }

    /**
     * 执行具体的任务处理逻辑，由子类实现
     */
    protected abstract suspend fun doExecute(task: T)

}