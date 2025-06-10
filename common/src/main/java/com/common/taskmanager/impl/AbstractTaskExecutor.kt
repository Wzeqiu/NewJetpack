package com.common.taskmanager.impl

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.mxm.douying.aigc.taskmanager.api.TaskExecutor
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
 * @param A 任务适配器类型
 */
abstract class AbstractTaskExecutor<T : Any, A : TaskAdapter<T>> : TaskExecutor,
    CoroutineScope {
    protected val TAG = this::class.java.simpleName

    // 使用SupervisorJob确保一个子协程失败不影响其他协程
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    // 互斥锁，用于保护共享状态
    private val mutex = Mutex()

    // 运行中的任务列表，按任务ID索引
    private val runningJobs = ArrayList<String>()

    private lateinit var callback: TaskCallback<Any>
    private lateinit var adapter: A

    override fun setCallBack(callback: TaskCallback<Any>) {
        this.callback = callback
    }

    override fun setAdapter(adapter: TaskAdapter<*>) {
        this.adapter = adapter as A
    }

    /**
     * 执行任务
     * 通用实现，处理基本的任务生命周期
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(task: Any) {
        // 检查任务类型是否匹配
        if (getTaskClass().isInstance(task)) {
            val typedTask = task as T
            // 执行具体业务逻辑
            executeTyped(typedTask)
        } else {
            throw IllegalArgumentException("任务类型不匹配: 期望 ${getTaskClass().simpleName}, 实际 ${task}")
        }
    }

    /**
     * 使用泛型执行任务
     * 子类可以直接调用此方法，而不需要处理泛型擦除
     */
    protected suspend fun executeTyped(task: T) {
        val taskId = adapter.getTaskId(task)
//        已存在的相同ID任务
        if (runningJobs.contains(taskId)) {
            LogUtils.w(TAG, "任务已存在: $taskId")
            return
        }

        mutex.withLock {
            // 标记任务开始执行
            adapter.markStarted(task)
            callback.onStatusChanged(task)
            // 创建并启动任务作业
            launch {
                try {
                    LogUtils.d(TAG, "开始执行任务: $taskId, 类型: ${adapter.getType(task)}")
                    doExecute(task)
                } catch (e: Exception) {
                    LogUtils.e(TAG, "任务执行异常: $taskId", e)
                    adapter.markFailure(task, e.message)
                    callback.onStatusChanged(task)
                    // 移除运行中的任务记录
                    runningJobs.remove(taskId)
                }
            }
            runningJobs.add(taskId)
        }
    }

    /**
     * 执行具体的任务处理逻辑，由子类实现
     * @param scope 协程作用域，子协程应该在此作用域内启动
     */
    protected abstract suspend fun doExecute(task: T)

    /**
     * 取消任务
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun cancel(task: Any): Boolean {
        if (getTaskClass().isInstance(task)) {
            return cancelTyped(task as T, adapter)
        }
        return false
    }

    /**
     * 使用泛型取消任务
     */
    protected suspend fun cancelTyped(task: T, adapter: A): Boolean {
        val taskId = adapter.getTaskId(task)
        return mutex.withLock {
            runningJobs.remove(taskId)
        }
    }

    /**
     * 取消全部任务
     */
    override suspend fun cancelAll(): Boolean {
        return mutex.withLock {
            //清空 runningJobs 并结束任务
            runningJobs.clear()
            return true
        }
    }

    /**
     * 更新任务失败状态
     */
    suspend fun updateTaskFailure(task: T) {
        cancel(task)
        adapter.markFailure(task)
        updateTaskStatus(task)
    }

    /**
     * 更新任务成功
     */
    suspend fun updateTaskSuccess(task: T) {
        cancel(task)
        adapter.markSuccess(task)
        updateTaskStatus(task)
    }

    /**
     * 跟新任务状态
     */
    override suspend fun updateTaskStatus(task: Any) {
        adapter.updateTask(task as T)
        callback.onStatusChanged(task)
    }

    /**
     * 判断任务是否在执行
     */
    protected fun isTaskRunning(taskId: String): Boolean {
        return runningJobs.contains(taskId)
    }

    /**
     * 判断特定任务是否在执行
     */
    protected fun isTaskRunning(task: T, adapter: A): Boolean {
        return isTaskRunning(adapter.getTaskId(task))
    }




} 