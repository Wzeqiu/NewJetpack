package com.common.taskmanager.impl

import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.ext.AITaskInfoAdapter
import com.common.taskmanager.ext.TextToImageExecutor
import com.common.taskmanager.api.TaskExecutor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

/**
 * 任务组件工厂
 * 统一管理适配器和执行器的创建和存储
 */
class TaskComponentFactory private constructor() {
    companion object {
        private const val TAG = "TaskComponentFactory"

        // 单例实例
        @Volatile
        private var instance: TaskComponentFactory? = null

        /**
         * 获取单例实例
         */
        @JvmStatic
        fun getInstance(): TaskComponentFactory {
            return instance ?: synchronized(this) {
                instance ?: TaskComponentFactory().also { instance = it }
            }
        }
    }

    // 读写锁，用于保护执行器和适配器的创建操作
    private val executorLock = ReentrantReadWriteLock()
    private val adapterLock = ReentrantReadWriteLock()

    // 任务类型到执行器类的映射
    private val executorMapping = mutableMapOf<Int, Class<out TaskExecutor<*>>>(
        TaskConstant.AI_TYPE_TEXT_TO_IMAGE to TextToImageExecutor::class.java,
        // 可以添加更多的任务类型到执行器的映射
    )

    // 任务类到适配器类的映射
    private val adapterMapping = mutableMapOf<Class<*>, Class<out TaskAdapter<*>>>(
        AITaskInfo::class.java to AITaskInfoAdapter::class.java,
        // 可以添加更多的任务类到适配器的映射
    )

    // 已创建的执行器实例
    private val executors = ConcurrentHashMap<Int, TaskExecutor<*>>()

    // 已创建的适配器实例
    private val adapters = ConcurrentHashMap<Class<*>, TaskAdapter<*>>()

    // 全局回调
    @Volatile
    private var globalCallback: TaskCallback<Any>? = null

    /**
     * 设置全局回调
     */
    fun setGlobalCallback(callback: TaskCallback<Any>) {
        this.globalCallback = callback
    }

    /**
     * 注册执行器类型
     * @param taskType 任务类型
     * @param executorClass 执行器类
     */
    fun registerExecutorClass(taskType: Int, executorClass: Class<out TaskExecutor<*>>) {
        executorMapping[taskType] = executorClass
        LogUtils.d(TAG, "注册执行器类: ${executorClass.simpleName} 用于任务类型: $taskType")
    }

    /**
     * 注册适配器类型
     * @param taskClass 任务类
     * @param adapterClass 适配器类
     */
    fun registerAdapterClass(taskClass: Class<*>, adapterClass: Class<out TaskAdapter<*>>) {
        adapterMapping[taskClass] = adapterClass
        LogUtils.d(
            TAG,
            "注册适配器类: ${adapterClass.simpleName} 用于任务类: ${taskClass.simpleName}"
        )
    }

    /**
     * 获取执行器
     * 先从缓存中查找，如果没有则创建新的执行器
     * 使用读写锁确保线程安全，提高并发性能
     * @param taskType 任务类型
     * @return 执行器实例，如果不支持该类型则返回null
     */
    fun getExecutor(taskType: Int): TaskExecutor<*>? {
        // 先从缓存中查找
        val executor = executors[taskType]
        if (executor != null) {
            return executor
        }
        // 没有找到已缓存的执行器，获取执行器类型
        val executorClass = executorMapping[taskType] ?: return null

        // 需要创建新的执行器，切换到写锁
        return executorLock.write {
            // 再次检查，避免其他线程已经创建了执行器
            executors.getOrPut(taskType) {
                // 确认没有缓存，创建新的执行器
                try {
                    val newExecutor = executorClass.getDeclaredConstructor().newInstance()
                    // 设置全局回调
                    globalCallback?.let { newExecutor.setCallBack(it) }

                    // 获取对应的适配器并设置
                    val taskClass = newExecutor.getTaskClass()
                    getAdapter(taskClass)?.let { adapter ->
                        // 使用类型安全的方式设置适配器
                        @Suppress("UNCHECKED_CAST")
                        val typedExecutor = newExecutor as TaskExecutor<Any>
                        val typedAdapter = adapter as TaskAdapter<Any>
                        typedExecutor.setAdapter(typedAdapter)
                    }

                    // 将创建的执行器放入缓存
                    LogUtils.d(
                        TAG,
                        "创建执行器: ${newExecutor.javaClass.simpleName} 用于任务类型: $taskType"
                    )
                    newExecutor
                } catch (e: Exception) {
                    LogUtils.e(TAG, "创建执行器失败: ${executorClass.name}", e)
                    null
                }
            }
        }
    }

    /**
     * 获取适配器
     * 先从缓存中查找，如果没有则创建新的适配器
     * 使用读写锁确保线程安全，提高并发性能
     * @param taskClass 任务类
     * @return 适配器实例，如果不支持该类型则返回null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAdapter(taskClass: Class<T>): TaskAdapter<T>? {
        // 先从缓存中查找
        val adapter = adapters[taskClass]
        if (adapter != null) {
            return adapter as TaskAdapter<T>
        }

        // 没有找到已缓存的适配器，获取适配器类型
        val adapterClass = adapterMapping[taskClass] ?: return null

        // 需要创建新的适配器，切换到写锁
        return adapterLock.write {
            // 再次检查，避免其他线程已经创建了适配器
            val existingAdapter = adapters[taskClass]
            if (existingAdapter != null) {
                return@write existingAdapter as TaskAdapter<T>
            }

            // 确认没有缓存，创建新的适配器
            try {
                val newAdapter =
                    adapterClass.getDeclaredConstructor().newInstance() as TaskAdapter<T>
                // 将创建的适配器放入缓存
                adapters[taskClass] = newAdapter as TaskAdapter<*>
                LogUtils.d(
                    TAG,
                    "创建适配器: ${newAdapter.javaClass.simpleName} 用于任务类: ${taskClass.simpleName}"
                )
                return@write newAdapter
            } catch (e: Exception) {
                LogUtils.e(TAG, "创建适配器失败: ${adapterClass.name}", e)
                return@write null
            }
        }
    }

    /**
     * 获取所有已创建的执行器
     */
    fun getAllExecutors(): Collection<TaskExecutor<*>> {
        return executors.values
    }

    /**
     * 获取所有已创建的适配器
     */
    fun getAllAdapters(): Collection<TaskAdapter<*>> {
        return adapters.values
    }

    /**
     * 获取指定任务类型的执行器
     */
    fun getExecutorForType(taskType: Int): TaskExecutor<*>? {
        return executors[taskType]
    }

    /**
     * 获取指定任务类的适配器
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAdapterForClass(taskClass: Class<T>): TaskAdapter<T>? {
        return adapters[taskClass] as? TaskAdapter<T>
    }

    /**
     * 清除所有缓存
     */
    fun clearCache() {
        executorLock.write {
            executors.clear()
        }
        adapterLock.write {
            adapters.clear()
        }
        LogUtils.d(TAG, "清除所有组件缓存")
    }

    /**
     * 获取所有支持的任务类型
     */
    fun getSupportedTaskTypes(): List<Int> {
        return executorMapping.keys.toList()
    }

    /**
     * 获取所有支持的任务类
     */
    fun getSupportedTaskClasses(): List<Class<*>> {
        return adapterMapping.keys.toList()
    }
} 