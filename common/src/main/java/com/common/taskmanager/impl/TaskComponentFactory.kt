package com.common.taskmanager.impl

import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.ext.AITaskInfoAdapter
import com.common.taskmanager.ext.TextToImageExecutor
import com.mxm.douying.aigc.taskmanager.api.TaskExecutor
import java.util.concurrent.ConcurrentHashMap

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
    
    // 任务类型到执行器类的映射
    private val executorMapping = mutableMapOf<Int, Class<out TaskExecutor>>(
        TaskConstant.AI_TYPE_TEXT_TO_IMAGE to TextToImageExecutor::class.java,
        // 可以添加更多的任务类型到执行器的映射
    )
    
    // 任务类到适配器类的映射
    private val adapterMapping = mutableMapOf<Class<*>, Class<out TaskAdapter<*>>>(
        AITaskInfo::class.java to AITaskInfoAdapter::class.java,
        // 可以添加更多的任务类到适配器的映射
    )
    
    // 已创建的执行器实例
    private val executors = ConcurrentHashMap<Int, TaskExecutor>()
    
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
    fun registerExecutorClass(taskType: Int, executorClass: Class<out TaskExecutor>) {
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
        LogUtils.d(TAG, "注册适配器类: ${adapterClass.simpleName} 用于任务类: ${taskClass.simpleName}")
    }
    
    /**
     * 获取执行器
     * 先从缓存中查找，如果没有则创建新的执行器
     * 使用双重检查锁定模式确保线程安全
     * @param taskType 任务类型
     * @return 执行器实例，如果不支持该类型则返回null
     */
    fun getExecutor(taskType: Int): TaskExecutor? {
        // 先从缓存中查找
        var executor = executors[taskType]
        if (executor != null) {
            return executor
        }
        
        // 使用同步块确保只有一个线程能创建执行器
        synchronized(executors) {
            // 再次检查，避免其他线程已经创建了执行器
            executor = executors[taskType]
            if (executor != null) {
                return executor
            }
            
            // 确认没有缓存，尝试创建新的执行器
            val executorClass = executorMapping[taskType] ?: return null
            
            try {
                executor = executorClass.getDeclaredConstructor().newInstance()
                // 设置全局回调
                globalCallback?.let { (executor as TaskExecutor).setCallBack(it) }
                
                // 获取对应的适配器并设置
                val taskClass = (executor as TaskExecutor).getTaskClass()
                getAdapter(taskClass)?.let { adapter ->
                    (executor as TaskExecutor).setAdapter(adapter)
                }
                
                // 将创建的执行器放入缓存
                executors[taskType] = executor as TaskExecutor
                LogUtils.d(TAG, "创建执行器: ${executor!!.javaClass.simpleName} 用于任务类型: $taskType")
                return executor
            } catch (e: Exception) {
                LogUtils.e(TAG, "创建执行器失败: ${executorClass.name}", e)
                return null
            }
        }
    }
    
    /**
     * 获取适配器
     * 先从缓存中查找，如果没有则创建新的适配器
     * 使用双重检查锁定模式确保线程安全
     * @param taskClass 任务类
     * @return 适配器实例，如果不支持该类型则返回null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getAdapter(taskClass: Class<T>): TaskAdapter<T>? {
        // 先从缓存中查找
        var adapter = adapters[taskClass]
        if (adapter != null) {
            return adapter as TaskAdapter<T>
        }
        
        // 使用同步块确保只有一个线程能创建适配器
        synchronized(adapters) {
            // 再次检查，避免其他线程已经创建了适配器
            adapter = adapters[taskClass]
            if (adapter != null) {
                return adapter as TaskAdapter<T>
            }
            
            // 确认没有缓存，尝试创建新的适配器
            val adapterClass = adapterMapping[taskClass] ?: return null
            
            try {
                adapter = adapterClass.getDeclaredConstructor().newInstance() as TaskAdapter<T>
                // 将创建的适配器放入缓存
                adapters[taskClass] = adapter as TaskAdapter<*>
                LogUtils.d(TAG, "创建适配器: ${adapter!!.javaClass.simpleName} 用于任务类: ${taskClass.simpleName}")
                return adapter as TaskAdapter<T>
            } catch (e: Exception) {
                LogUtils.e(TAG, "创建适配器失败: ${adapterClass.name}", e)
                return null
            }
        }
    }
    
    /**
     * 获取所有已创建的执行器
     */
    fun getAllExecutors(): Collection<TaskExecutor> {
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
    fun getExecutorForType(taskType: Int): TaskExecutor? {
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
        synchronized(executors) {
            executors.clear()
        }
        synchronized(adapters) {
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