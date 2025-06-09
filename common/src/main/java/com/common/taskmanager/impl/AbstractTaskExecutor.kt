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
 * @param T 任务对象类型
 * @param A 任务适配器类型
 */
abstract class AbstractTaskExecutor<T : Any, A : TaskAdapter<T>> : TaskExecutor<T, A>,
    CoroutineScope {
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
    @Suppress("UNCHECKED_CAST")
    override suspend fun <U> execute(task: U, adapter: TaskAdapter<U>, callback: TaskCallback<U>) {
        // 检查任务类型是否匹配
        if (getTaskClass().isInstance(task)) {
            val typedTask = task as T
            // 检查适配器类型是否匹配
            if (this.adapter::class.java.isAssignableFrom(adapter::class.java)) {
                executeTyped(typedTask, callback as TaskCallback<T>)
            } else {
                throw IllegalArgumentException("适配器类型不匹配: 期望 ${this.adapter::class.java.simpleName}, 实际 ${adapter::class.java.simpleName}")
            }
        } else {
            throw IllegalArgumentException("任务类型不匹配: 期望 ${getTaskClass().simpleName}, 实际 ${task}")
        }
    }

    /**
     * 使用泛型执行任务
     * 子类可以直接调用此方法，而不需要处理泛型擦除
     */
    override suspend fun executeTyped(task: T, callback: TaskCallback<T>) {
        val taskId = adapter.getTaskId(task)

        // 取消已存在的相同ID任务
        cancelTyped(task)

        mutex.withLock {
            // 创建并启动任务作业
            val job = launch {
                try {
                    LogUtils.d(TAG, "开始执行任务: $taskId, 类型: ${adapter.getType(task)}")

                    // 标记任务开始执行
                    adapter.markStarted(task)
                    callback.onStatusChanged(task)

                    // 执行具体的任务处理逻辑
                    doExecute(task, callback)

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
     * 现在使用强类型参数，子类不需要进行类型转换
     */
    protected abstract suspend fun doExecute(
        task: T,
        callback: TaskCallback<T>
    )

    /**
     * 取消任务
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun <U> cancel(task: U, adapter: TaskAdapter<U>): Boolean {
        if (getTaskClass().isInstance(task)) {
            return cancelTyped(task as T)
        }
        return false
    }

    /**
     * 使用泛型取消任务
     */
    override suspend fun cancelTyped(task: T): Boolean {
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

    /**
     * 判断特定任务是否在执行
     */
    protected fun isTaskRunning(task: T): Boolean {
        return isTaskRunning(adapter.getTaskId(task))
    }

    /**
     * 获取任务类类型
     * 默认实现，子类可以覆盖
     */
    override fun getTaskClass(): Class<T> {
        // 这里使用了泛型反射技巧，需要子类可能需要重写此方法提供准确的Class
        @Suppress("UNCHECKED_CAST")
        return javaClass.genericSuperclass.let {
            if (it is java.lang.reflect.ParameterizedType) {
                it.actualTypeArguments[0] as Class<T>
            } else {
                throw RuntimeException("需要重写getTaskClass()方法提供具体的任务类型")
            }
        }
    }
} 