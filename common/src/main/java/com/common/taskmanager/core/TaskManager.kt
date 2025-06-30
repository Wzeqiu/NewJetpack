package com.common.taskmanager.core

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.api.TaskEvent
import com.common.taskmanager.api.TaskEventListener
import com.common.taskmanager.impl.ListenerManager
import com.common.taskmanager.impl.TaskComponentFactory
import com.common.taskmanager.api.TaskExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * 统一任务管理器
 * 支持管理不同类型的任务对象
 * 采用懒加载方式创建适配器和执行器
 */
class TaskManager private constructor() : CoroutineScope {
    companion object {
        const val TAG = "TaskManager"

        @Volatile
        private var instance: TaskManager? = null

        /**
         * 获取TaskManager的单例实例
         */
        @JvmStatic
        fun getInstance(): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager().also { instance = it }
            }
        }
    }

    // 协程作用域
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.IO + supervisorJob

    // 监听器管理器
    private val listenerManager = ListenerManager()

    // 组件工厂
    private val componentFactory = TaskComponentFactory.getInstance()

    // 全局共享回调实例
    private val sharedCallback = object : TaskCallback<Any> {
        override fun onStatusChanged(task: Any) {
            launch {
                val adapter = componentFactory.getAdapterForClass(task.javaClass) ?: return@launch
                // 更新数据库
                adapter.updateTask(task)
                // 通知监听器
                val event = TaskEvent(task, adapter)
                listenerManager.notifyTaskStatusChanged(event)
            }
        }
    }

    init {
        // 设置组件工厂的全局回调
        componentFactory.setGlobalCallback(sharedCallback)
    }

    /**
     * 添加任务
     * @param task 任务对象
     */
    fun <T : Any> addTask(task: T) {
        launch {
            runCatching {
                // 获取或创建适配器
                val adapter = componentFactory.getAdapter(task.javaClass) as? TaskAdapter<T>
                    ?: throw IllegalArgumentException("未找到任务适配器且无法创建: ${task.javaClass.simpleName}")
                val taskId = adapter.getTaskId(task)
                // 只锁定执行任务和发送通知的部分
                if (adapter.isActive(task)) {
                    executeTask(task, adapter)
                    // 通知任务添加 - 可以在锁外执行
                    val event = TaskEvent(task, adapter)
                    listenerManager.notifyTaskAdded(event)

                    LogUtils.d(
                        TAG,
                        "任务已添加: $taskId, 类型: ${adapter.getType(task)}"
                    )
                }
            }.onFailure {
                LogUtils.e(TAG, "添加任务异常", it)
            }
        }
    }

    /**
     * 执行任务
     * @param task 任务对象
     * @param adapter 适配器
     */
    private fun <T : Any> executeTask(task: T, adapter: TaskAdapter<T>) {
        val taskType = adapter.getType(task)
        val rawExecutor = componentFactory.getExecutor(taskType)

        if (rawExecutor == null) {
            LogUtils.e(TAG, "未找到任务类型对应的执行器且无法创建: $taskType")
            adapter.markFailure(task)
            sharedCallback.onStatusChanged(task)
            return
        }

        // 使用类型安全的方式执行任务
        @Suppress("UNCHECKED_CAST")
        val executor = rawExecutor as TaskExecutor<T>

        launch {
            kotlin.runCatching {
                executor.execute(task)
            }.onFailure {
                LogUtils.e(TAG, "执行任务异常: ${adapter.getTaskId(task)}", it)
                adapter.markFailure(task)
                sharedCallback.onStatusChanged(task)
            }
        }
    }

    /**
     * 取消任务
     * @param task 任务对象
     */
    fun <T : Any> cancelTask(task: T): Boolean {
        val adapter = componentFactory.getAdapter(task.javaClass) as? TaskAdapter<T> ?: return false
        val taskId = adapter.getTaskId(task)
        val taskType = adapter.getType(task)

        val rawExecutor = componentFactory.getExecutor(taskType) ?: return false

        // 使用类型安全的方式取消任务
        @Suppress("UNCHECKED_CAST")
        val executor = rawExecutor as TaskExecutor<T>

        launch {
            if (executor.cancel(task)) {
                LogUtils.d(TAG, "任务已取消: $taskId")
            }
        }

        return true
    }

    /**
     * 取消所有任务
     */
    fun cancelAllTask(): Boolean {
        launch {
            componentFactory.getAllExecutors().forEach {
                it.cancelAll()
            }
        }
        return true
    }


    /**
     * 刷新任务列表（从数据库加载所有任务）
     */
    fun refreshTasks() {
        launch {
            componentFactory.getAllAdapters().forEach { adapter ->
                @Suppress("UNCHECKED_CAST")
                val typedAdapter = adapter as TaskAdapter<Any>
                val tasks = typedAdapter.loadAllTasks()
                val activeTasks = tasks.filter { typedAdapter.isActive(it) }
                if (activeTasks.isNotEmpty()) {
                    activeTasks.forEach { task ->
                        executeTask(task, typedAdapter)
                    }
                }

                LogUtils.d(
                    TAG,
                    "已刷新 ${adapter.javaClass.simpleName} 的任务：总数 ${tasks.size}，活跃 ${activeTasks.size}"
                )
            }
        }
    }

    /**
     * 获取特定类型的任务列表
     * @param taskType 任务类型
     */
    suspend fun <T : Any> getTasksByType(clazz: Class<T>, taskType: Int): List<T> {
        val adapter = componentFactory.getAdapter(clazz) ?: return emptyList()
        return adapter.loadAllTasks().filter { adapter.getType(it) == taskType }
    }

    /**
     * 获取待处理任务列表
     */
    suspend fun <T : Any> getPendingTasks(clazz: Class<T>): List<T> {
        val adapter = componentFactory.getAdapter(clazz) ?: return emptyList()
        return adapter.loadAllTasks().filter { adapter.isActive(it) }
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
        listenerManager.clear()
        componentFactory.clearCache()
        instance = null
        LogUtils.d(TAG, "任务管理器已销毁")
    }

    /**
     * 注册适配器类型
     * @param taskClass 任务类
     * @param adapterClass 适配器类
     */
    fun registerAdapterClass(taskClass: Class<*>, adapterClass: Class<out TaskAdapter<*>>) {
        componentFactory.registerAdapterClass(taskClass, adapterClass)
    }

    /**
     * 注册执行器类型
     * @param taskType 任务类型
     * @param executorClass 执行器类
     */
    fun registerExecutorClass(taskType: Int, executorClass: Class<out TaskExecutor<*>>) {
        componentFactory.registerExecutorClass(taskType, executorClass)
    }
}