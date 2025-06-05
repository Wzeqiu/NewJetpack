package com.common.taskmanager.core

import com.blankj.utilcode.util.LogUtils
import com.common.db.DbManager
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.executor.OralBroadcastingExecutor
import com.common.taskmanager.executor.PictureToVideoExecutor
import com.common.taskmanager.executor.TaskExecutor
import com.common.taskmanager.executor.TextToImageExecutor
import com.common.taskmanager.listener.TaskLifecycleListener
import com.common.taskmanager.listener.TaskListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 任务管理器
 * 负责管理所有AI任务的创建、执行、状态更新和删除
 */
class TaskManager private constructor() : CoroutineScope {
    companion object {
        private const val TAG = "TaskManager"

        @Volatile
        private var instance: TaskManager? = null

        /**
         * 获取任务管理器实例
         */
        fun getInstance(): TaskManager {
            return instance ?: synchronized(this) {
                instance ?: TaskManager().also { instance = it }
            }
        }
    }

    // 协程作用域
    private val supervisorJob = SupervisorJob()
    override val coroutineContext = Dispatchers.IO + supervisorJob

    // 任务执行器映射
    private val executors = ConcurrentHashMap<Int, TaskExecutor>()

    // 任务列表
    private val taskList = CopyOnWriteArrayList<AITaskInfo>()

    // 状态监听器列表
    private val statusListeners = CopyOnWriteArrayList<TaskListener>()
    private val lifecycleListeners = CopyOnWriteArrayList<TaskLifecycleListener>()

    // 互斥锁，保证任务操作的线程安全
    private val mutex = Mutex()

    // 初始化任务执行器
    init {
        // 注册各类任务执行器
        registerExecutor(TextToImageExecutor())
        registerExecutor(PictureToVideoExecutor())
        registerExecutor(OralBroadcastingExecutor())

        LogUtils.d(TAG, "任务管理器已初始化")
    }

    /**
     * 注册任务执行器
     */
    private fun registerExecutor(executor: TaskExecutor) {
        executor.getSupportedTaskTypes().forEach { type ->
            executors[type] = executor
        }
    }

    /**
     * 获取指定类型的任务执行器
     */
    private fun getExecutor(taskType: Int): TaskExecutor? {
        return executors[taskType]
    }

    /**
     * 添加任务
     */
    fun addTask(task: AITaskInfo) {
        launch {
            // 保存任务到数据库
            DbManager.taskInfoDao.insert(task)

            mutex.withLock {
                // 添加到内存列表
                taskList.add(task)

                // 如果任务状态为创建中，立即执行
                if (task.isCreate()) {
                    executeTask(task)
                }

                // 通知监听器
                lifecycleListeners.forEach { it.onTaskAdded(task) }
            }
        }
    }

    /**
     * 执行任务
     */
    private fun executeTask(task: AITaskInfo) {
        val executor = getExecutor(task.type)
        if (executor == null) {
            LogUtils.e(TAG, "未找到任务类型对应的执行器: ${task.type}")
            return
        }

        launch {
            executor.executeTask(task, object : TaskListener {
                override fun onTaskStatusChanged(task: AITaskInfo) {
                    // 更新数据库
                    launch {
                        DbManager.taskInfoDao.update(task)

                        // 通知所有监听器
                        statusListeners.forEach { it.onTaskStatusChanged(task) }
                    }
                }

                override fun onTaskProgressUpdated(task: AITaskInfo, progress: Int) {
                    // 通知所有监听器
                    statusListeners.forEach { it.onTaskProgressUpdated(task, progress) }
                }
            })
        }
    }

    /**
     * 取消任务
     */
    fun cancelTask(task: AITaskInfo): Boolean {
        val executor = getExecutor(task.type) ?: return false

        launch {
            if (executor.cancelTask(task)) {
                LogUtils.d(TAG, "任务已取消: ${task.taskId}")
            }
        }

        return true
    }

    /**
     * 删除任务
     */
    fun deleteTasks(tasks: List<AITaskInfo>) {
        if (tasks.isEmpty()) return

        launch {
            mutex.withLock {
                // 取消正在执行的任务
                tasks.forEach { task ->
                    // 标记为删除状态
                    task.status = TaskType.TASK_STATUS_DELETE
                    cancelTask(task)
                }

                // 从内存列表中移除
                taskList.removeAll(tasks)

                // 从数据库中删除
                DbManager.taskInfoDao.delete(tasks)

                // 通知监听器
                lifecycleListeners.forEach { it.onTaskRemoved(tasks) }
            }
        }
    }

    /**
     * 刷新任务列表（从数据库加载）
     */
    fun refreshTasks() {
        launch {
            val tasks = DbManager.taskInfoDao.getAll()
            mutex.withLock {
                // 清空当前列表
                val previousTaskIds = taskList.map { it.taskId }.toSet()
                taskList.clear()
                taskList.addAll(tasks)

                // 执行待处理的任务
                tasks.filter { it.isCreate() }.forEach { task ->
                    if (!previousTaskIds.contains(task.taskId)) {
                        executeTask(task)
                    }
                }

                LogUtils.d(TAG, "已刷新任务列表，共${tasks.size}个任务")
            }
        }
    }

    /**
     * 获取任务列表
     */
    fun getTasks(): List<AITaskInfo> {
        return taskList.toList()
    }

    /**
     * 获取特定类型的任务列表
     */
    fun getTasksByType(taskType: Int): List<AITaskInfo> {
        return taskList.filter { it.type == taskType }
    }

    /**
     * 获取待处理任务列表
     */
    fun getPendingTasks(): List<AITaskInfo> {
        return taskList.filter { it.status == TaskType.TASK_STATUS_CREATE }
    }

    /**
     * 添加任务状态监听器
     */
    fun addTaskStatusListener(listener: TaskListener) {
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener)
        }
    }

    /**
     * 移除任务状态监听器
     */
    fun removeTaskStatusListener(listener: TaskListener) {
        statusListeners.remove(listener)
    }

    /**
     * 添加任务生命周期监听器
     */
    fun addTaskLifecycleListener(listener: TaskLifecycleListener) {
        if (!lifecycleListeners.contains(listener)) {
            lifecycleListeners.add(listener)
        }
    }

    /**
     * 移除任务生命周期监听器
     */
    fun removeTaskLifecycleListener(listener: TaskLifecycleListener) {
        lifecycleListeners.remove(listener)
    }

    /**
     * 清理资源
     */
    fun destroy() {
        supervisorJob.cancel()
        executors.clear()
        statusListeners.clear()
        lifecycleListeners.clear()
        taskList.clear()
        instance = null
        LogUtils.d(TAG, "任务管理器已销毁")
    }
} 