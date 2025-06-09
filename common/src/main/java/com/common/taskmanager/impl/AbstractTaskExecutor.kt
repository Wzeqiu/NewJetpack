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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
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
    private val runningJobs = ConcurrentHashMap<String, Job>()

    /**
     * 执行任务
     * 通用实现，处理基本的任务生命周期
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(task: Any, adapter: TaskAdapter<*>, callback: TaskCallback<*>) {
        // 检查任务类型是否匹配
        if (getTaskClass().isInstance(task)) {
            val typedTask = task as T
            val typedAdapter = adapter as A
            val typedCallback = callback as TaskCallback<T>

            // 执行具体业务逻辑
            executeTyped(typedTask, typedAdapter, typedCallback)
        } else {
            throw IllegalArgumentException("任务类型不匹配: 期望 ${getTaskClass().simpleName}, 实际 ${task}")
        }
    }

    /**
     * 使用泛型执行任务
     * 子类可以直接调用此方法，而不需要处理泛型擦除
     */
    protected suspend fun executeTyped(task: T, adapter: A, callback: TaskCallback<T>) {
        val taskId = adapter.getTaskId(task)

        // 取消已存在的相同ID任务
        cancelTyped(task, adapter)

        mutex.withLock {
            // 标记任务开始执行
            adapter.markStarted(task)
            callback.onStatusChanged(task)

            // 创建并启动任务作业
            val job = launch {
                try {
                    LogUtils.d(TAG, "开始执行任务: $taskId, 类型: ${adapter.getType(task)}")

                    // 使用coroutineScope确保所有子协程完成后才结束
                    coroutineScope {
                        // 执行具体的任务处理逻辑，同时传递当前协程作用域
                        doExecute(task, adapter, callback, this)
                    }

                    LogUtils.d(TAG, "任务执行完成: $taskId")
                } catch (e: CancellationException) {
                    LogUtils.w(TAG, "任务已取消: $taskId")
                    throw e
                } catch (e: Exception) {
                    LogUtils.e(TAG, "任务执行异常: $taskId", e)
                    adapter.markFailure(task, e.message)
                    callback.onStatusChanged(task)
                } finally {
                    // 移除运行中的任务记录
                    runningJobs.remove(taskId)
                }
            }

            runningJobs[taskId] = job

            // 等待作业完成，而不是立即返回
            job.join()
        }
    }

    /**
     * 执行具体的任务处理逻辑，由子类实现
     * @param scope 协程作用域，子协程应该在此作用域内启动
     */
    protected abstract suspend fun doExecute(
        task: T,
        adapter: A,
        callback: TaskCallback<T>,
        scope: CoroutineScope = this
    )

    /**
     * 取消任务
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun cancel(task: Any, adapter: TaskAdapter<*>): Boolean {
        if (getTaskClass().isInstance(task)) {
            return cancelTyped(task as T, adapter as A)
        }
        return false
    }

    /**
     * 使用泛型取消任务
     */
    protected suspend fun cancelTyped(task: T, adapter: A): Boolean {
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
    protected fun isTaskRunning(task: T, adapter: A): Boolean {
        return isTaskRunning(adapter.getTaskId(task))
    }

    /**
     * 获取任务类类型
     * 默认实现，子类可以覆盖
     */
    fun getTaskClass(): Class<T> {
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