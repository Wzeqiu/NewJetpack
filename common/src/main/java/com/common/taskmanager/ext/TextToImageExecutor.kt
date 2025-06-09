package com.common.taskmanager.ext

import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.impl.AbstractTaskExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 文生图任务执行器
 * 专门处理AITaskInfo类型的文生图任务
 */
class TextToImageExecutor : AbstractTaskExecutor<AITaskInfo, AITaskInfoAdapter>() {

    override suspend fun doExecute(
        task: AITaskInfo,
        adapter: AITaskInfoAdapter,
        callback: TaskCallback<AITaskInfo>
    ) {
        LogUtils.d(TAG, "开始执行文生图任务: ${task.taskId}")
        
        // 模拟任务执行过程
        for (progress in 0..100 step 5) {
            // 检查是否被取消
            if (!isActive) {
                LogUtils.d(TAG, "文生图任务被取消: ${task.taskId}")
                return
            }
            
            // 更新进度
            adapter.setProgress(task, progress)
            callback.onProgressUpdate(task, progress)
            
            // 延迟一段时间模拟处理
            delay(300)
        }
        
        // 设置任务成功
        val resultUrl = "https://example.com/generated_image_${System.currentTimeMillis()}.jpg"
        adapter.markSuccess(task, resultUrl)
        callback.onSuccess(task, resultUrl)
        
        LogUtils.d(TAG, "文生图任务执行成功: ${task.taskId}, 结果: $resultUrl")
    }
    
    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskConstant.AI_TYPE_TEXT_TO_IMAGE
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskConstant.AI_TYPE_TEXT_TO_IMAGE)
    }

}