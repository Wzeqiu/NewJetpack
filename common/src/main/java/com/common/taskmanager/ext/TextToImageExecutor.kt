package com.common.taskmanager.ext

import com.blankj.utilcode.util.LogUtils
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.impl.AbstractTaskExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 文生图任务执行器
 */
class TextToImageExecutor : AbstractTaskExecutor() {
    
    override suspend fun <T> doExecute(
        task: T,
        adapter: TaskAdapter<T>,
        callback: TaskCallback<T>
    ) {
        LogUtils.d(TAG, "开始执行文生图任务: ${adapter.getTaskId(task)}")
        
        // 模拟任务执行过程
        for (progress in 0..100 step 5) {
            // 检查是否被取消
            if (!isActive) {
                LogUtils.d(TAG, "文生图任务被取消: ${adapter.getTaskId(task)}")
                return
            }
            
            // 更新进度
            adapter.setProgress(task, progress)
            callback.onProgressUpdate(task, progress)
            
            // 延迟一段时间模拟处理
            delay(300)
        }
        
        // 设置任务成功
        adapter.markSuccess(task, "生成的图片URL或路径")
        callback.onSuccess(task, "生成的图片URL或路径")
        
        LogUtils.d(TAG, "文生图任务执行成功: ${adapter.getTaskId(task)}")
    }
    
    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskConstant.AI_TYPE_TEXT_TO_IMAGE
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskConstant.AI_TYPE_TEXT_TO_IMAGE)
    }
} 