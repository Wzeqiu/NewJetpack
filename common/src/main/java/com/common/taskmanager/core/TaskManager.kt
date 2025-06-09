package com.common.taskmanager.core

import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.api.TaskEvent
import com.common.taskmanager.api.TaskEventListener
import com.common.taskmanager.api.TaskExecutor
import com.common.taskmanager.ext.AITaskInfoAdapter
import com.common.taskmanager.ext.TextToImageExecutor
import com.common.taskmanager.ext.VideoTaskExecutor
import com.common.taskmanager.impl.ListenerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * 统一任务管理器
 * 支持管理不同类型的任务对象
 */
class TaskManager : CoroutineScope {
    companion object {
        const val TAG = "TaskManager"

        @Volatile
        private var instance: TaskManager? = null

        /**
         * 获取TaskManager的单例实例
         */
        fun getInstance(): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager().also { instance = it }
            }
        }
    }

    // 协程作用域
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + supervisorJob

    // 类型到适配器的映射
    private val adapters = ConcurrentHashMap<Class<*>, TaskAdapter<*>>()

    // 任务类型到执行器的映射
    private val executors = ConcurrentHashMap<Int, TaskExecutor<*, *>>()

    // 监听器管理器
    private val listenerManager = ListenerManager()

    // 互斥锁
    private val mutex = Mutex()

    init {
        // 注册执行器
        val adapter = AITaskInfoAdapter()
        registerAdapter<AITaskInfo>(adapter)
        registerExecutor(TextToImageExecutor(adapter))
        registerExecutor(VideoTaskExecutor(adapter))
    }

    /**
     * 注册任务适配器
     * @param clazz 任务类的Class对象
     * @param adapter 对应的任务适配器
     */
    fun <T : Any> registerAdapter(clazz: Class<T>, adapter: TaskAdapter<T>) {
        adapters[clazz] = adapter
        LogUtils.d(TAG, "注册任务适配器: ${clazz.simpleName}")
    }

    /**
     * 通过泛型方式注册任务适配器
     * @param adapter 对应的任务适配器
     */
    inline fun <reified T : Any> registerAdapter(adapter: TaskAdapter<T>) {
        registerAdapter(T::class.java, adapter)
        LogUtils.d(TAG, "通过泛型注册任务适配器: ${T::class.java.simpleName}")
    }

    /**
     * 注册任务执行器
     * @param executor 任务执行器
     */
    fun registerExecutor(executor: TaskExecutor<*,*>) {
        for (taskType in executor.getSupportedTaskTypes()) {
            executors[taskType] = executor
        }
        LogUtils.d(
            TAG,
            "注册任务执行器: ${executor.javaClass.simpleName}, 支持任务类型: ${executor.getSupportedTaskTypes()}"
        )
    }

    /**
     * 添加任务
     * @param task 任务对象
     */
    fun <T : Any> addTask(task: T) {
        val adapter = getAdapter(task)
            ?: throw IllegalArgumentException("未找到任务适配器: ${task.javaClass.simpleName}")

        launch {
            // 保存任务到数据库
            adapter.saveTask(task)

            mutex.withLock {
                // 如果任务状态为创建中，立即执行
                if (adapter.getStatus(task) == TaskConstant.TASK_STATUS_CREATE) {
                    executeTask(task, adapter)
                }

                // 通知监听器
                val event = TaskEvent(task, adapter)
                listenerManager.notifyTaskAdded(event)

                LogUtils.d(
                    TAG,
                    "任务已添加: ${adapter.getTaskId(task)}, 类型: ${adapter.getType(task)}"
                )
            }
        }
    }

    /**
     * 执行任务
     * @param task 任务对象
     * @param adapter 适配器
     */
    private fun <T> executeTask(task: T, adapter: TaskAdapter<T>) {
        val taskType = adapter.getType(task)
        val executor = executors[taskType]

        if (executor == null) {
            LogUtils.e(TAG, "未找到任务类型对应的执行器: $taskType")
            adapter.markFailure(task, "未找到执行器")
            return
        }

        launch {
            try {
                executor.execute(task, adapter, object : TaskCallback<T> {
                    override fun onStatusChanged(task: T) {
                        launch {
                            // 更新数据库
                            adapter.updateTask(task)

                            // 通知监听器
                            val event = TaskEvent(task, adapter)
                            listenerManager.notifyTaskStatusChanged(event)
                        }
                    }
                })
            } catch (e: Exception) {
                LogUtils.e(TAG, "执行任务异常: ${adapter.getTaskId(task)}", e)
                adapter.markFailure(task, e.message)
            }
        }
    }

    /**
     * 取消任务
     * @param task 任务对象
     */
    fun <T : Any> cancelTask(task: T): Boolean {
        val adapter = getAdapter(task) ?: return false
        val executor = executors[adapter.getType(task)] ?: return false

        launch {
            if (executor.cancel(task, adapter)) {
                LogUtils.d(TAG, "任务已取消: ${adapter.getTaskId(task)}")
            }
        }

        return true
    }

    /**
     * 删除任务
     * @param tasks 任务对象列表
     */
    fun <T : Any> deleteTasks(tasks: List<T>) {
        if (tasks.isEmpty()) return

        // 按适配器分组
        val tasksByAdapter = tasks.groupBy { it.javaClass }

        launch {
            mutex.withLock {
                val events = mutableListOf<TaskEvent<*>>()

                tasksByAdapter.forEach { (taskClass, typeTasks) ->
                    val adapter = adapters[taskClass] ?: return@forEach

                    // 取消正在执行的任务
                    typeTasks.forEach { task ->
                        cancelTask(task as Any)

                        @Suppress("UNCHECKED_CAST")
                        val typedTask = task as T
                        val typedAdapter = adapter as TaskAdapter<T>

                        // 标记为删除状态
                        typedAdapter.setStatus(typedTask, TaskConstant.TASK_STATUS_DELETE)

                        // 收集事件
                        events.add(TaskEvent(typedTask, typedAdapter))
                    }

                    // 从数据库中删除
                    @Suppress("UNCHECKED_CAST")
                    (adapter as TaskAdapter<Any>).deleteTask(typeTasks as Any)
                }

                // 通知监听器
                if (events.isNotEmpty()) {
                    listenerManager.notifyTasksRemoved(events)
                }
            }
        }
    }

    /**
     * 刷新任务列表（从数据库加载所有任务）
     */
    fun refreshTasks() {
        launch {
            adapters.forEach { (_, adapter) ->
                @Suppress("UNCHECKED_CAST")
                val typedAdapter = adapter as TaskAdapter<Any>

                val tasks = typedAdapter.loadAllTasks()

                // 查找并执行待处理的任务
                tasks.filter { typedAdapter.getStatus(it) == TaskConstant.TASK_STATUS_CREATE }
                    .forEach { task ->
                        executeTask(task, typedAdapter)
                    }
            }

            LogUtils.d(TAG, "已刷新所有任务")
        }
    }

    /**
     * 获取特定类型的任务列表
     * @param taskType 任务类型
     */
    suspend fun <T : Any> getTasksByType(clazz: Class<T>, taskType: Int): List<T> {
        val adapter = adapters[clazz] as? TaskAdapter<T> ?: return emptyList()
        return adapter.loadAllTasks().filter { adapter.getType(it) == taskType }
    }

    /**
     * 获取待处理任务列表
     */
    suspend fun <T : Any> getPendingTasks(clazz: Class<T>): List<T> {
        val adapter = adapters[clazz] as? TaskAdapter<T> ?: return emptyList()
        return adapter.loadAllTasks()
            .filter { adapter.getStatus(it) == TaskConstant.TASK_STATUS_CREATE }
    }

    /**
     * 添加任务监听器（监听所有类型任务）
     */
    fun addTaskListener(listener: TaskEventListener) {
        listenerManager.addListener(listener)
    }

    /**
     * 添加特定类型的任务监听器
     */
    fun addTaskListener(listener: TaskEventListener, @TaskConstant.Type taskType: Int) {
        listenerManager.addListener(listener, taskType)
    }

    /**
     * 移除任务监听器
     */
    fun removeTaskListener(listener: TaskEventListener) {
        listenerManager.removeListener(listener)
    }

    /**
     * 清理资源
     */
    fun destroy() {
        supervisorJob.cancel()
        executors.clear()
        adapters.clear()
        listenerManager.clear()
        instance = null
        LogUtils.d(TAG, "任务管理器已销毁")
    }

    /**
     * 获取任务适配器
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> getAdapter(task: T): TaskAdapter<T>? {
        return adapters[task.javaClass] as? TaskAdapter<T>
    }
}