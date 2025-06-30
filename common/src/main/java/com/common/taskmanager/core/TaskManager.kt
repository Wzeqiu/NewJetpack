package com.common.taskmanager.core

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.api.TaskEvent
import com.common.taskmanager.api.TaskEventListener
import com.common.taskmanager.impl.ListenerManager
import com.common.taskmanager.impl.TaskComponentFactory
import com.mxm.douying.aigc.taskmanager.api.TaskExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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

    // 互斥锁
    private val mutex = Mutex()

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
            try {
                // 获取或创建适配器
                val adapter = componentFactory.getAdapter(task.javaClass) as? TaskAdapter<T>
                    ?: throw IllegalArgumentException("未找到任务适配器且无法创建: ${task.javaClass.simpleName}")
                
                val taskId = adapter.getTaskId(task)
                
                // 检查任务是否已存在并处于活跃状态
                val existingTask = adapter.findTask(taskId)
                if (existingTask != null && adapter.isActive(existingTask)) {
                    LogUtils.w(TAG, "任务已存在并处于活跃状态，跳过添加: $taskId")
                    return@launch
                }
                
                // 保存任务到数据库
                adapter.saveTask(task)

                mutex.withLock {
                    // 如果任务状态为活跃状态，立即执行
                    if (adapter.isActive(task)) {
                        executeTask(task, adapter)
                    }

                    // 通知监听器
                    val event = TaskEvent(task, adapter)
                    listenerManager.notifyTaskAdded(event)

                    LogUtils.d(
                        TAG,
                        "任务已添加: $taskId, 类型: ${adapter.getType(task)}"
                    )
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "添加任务异常", e)
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
        val executor = componentFactory.getExecutor(taskType)

        if (executor == null) {
            LogUtils.e(TAG, "未找到任务类型对应的执行器且无法创建: $taskType")
            adapter.markFailure(task, "未找到执行器")
            sharedCallback.onStatusChanged(task)
            return
        }

        launch {
            kotlin.runCatching {
                executor.execute(task)
            }.onFailure {
                LogUtils.e(TAG, "执行任务异常: ${adapter.getTaskId(task)}", it)
                adapter.markFailure(task, it.message)
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
        
        // 检查任务是否处于活跃状态
        if (!adapter.isActive(task)) {
            LogUtils.d(TAG, "任务不在活跃状态，无需取消: $taskId")
            return false
        }
        
        val executor = componentFactory.getExecutor(taskType) ?: return false

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
                    val adapter = componentFactory.getAdapter(taskClass) ?: return@forEach

                    // 取消正在执行的任务
                    typeTasks.forEach { task ->
                        cancelTask(task)
                        @Suppress("UNCHECKED_CAST")
                        val typedAdapter = adapter as TaskAdapter<T>
                        // 标记为删除状态
                        typedAdapter.markDelete(task)
                        // 收集事件
                        events.add(TaskEvent(task, typedAdapter))
                    }
                    // 从数据库中删除
                    adapter.deleteTask(typeTasks)
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
            componentFactory.getAllAdapters().forEach { adapter ->
                @Suppress("UNCHECKED_CAST")
                val typedAdapter = adapter as TaskAdapter<Any>

                val tasks = typedAdapter.loadAllTasks()

                // 查找并执行待处理的任务
                tasks.filter { typedAdapter.isActive(it) }
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
    fun registerExecutorClass(taskType: Int, executorClass: Class<out TaskExecutor>) {
        componentFactory.registerExecutorClass(taskType, executorClass)
    }
}