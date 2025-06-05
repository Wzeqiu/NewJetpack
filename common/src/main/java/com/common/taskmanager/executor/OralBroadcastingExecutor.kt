package com.common.taskmanager.executor

import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.network.httpServer
import com.common.taskmanager.core.TaskType
import com.common.taskmanager.listener.TaskListener
import kotlinx.coroutines.delay

/**
 * AI口播任务执行器
 */
class OralBroadcastingExecutor : VideoTaskExecutor() {
    companion object {
        private const val MAX_AUDIO_PROCESSING_RETRIES = 3 // 最大音频处理重试次数
        private const val AUDIO_CHECK_INTERVAL = 5000L // 音频检查间隔时间(毫秒)
    }
    
    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskType.AI_TYPE_ORAL_BROADCASTING
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskType.AI_TYPE_ORAL_BROADCASTING)
    }
    
    /**
     * 处理任务特定成功
     * 在父类成功处理基础上，添加AI口播特有的处理逻辑
     */
    override fun handleTaskSpecificSuccess(task: AITaskInfo) {
        super.handleTaskSpecificSuccess(task)
        
        // 更新任务状态和结果信息
        task.apply {
            status = TaskType.TASK_STATUS_SUCCESS
            // 假设在下载成功后，结果已经设置到task.result中
            size = if (result?.isNotEmpty() == true) {
                val file = java.io.File(result)
                if (file.exists()) {
                    ConvertUtils.byte2FitMemorySize(file.length(), 1)
                } else {
                    "未知大小"
                }
            } else {
                "未知大小"
            }
            
        }
        
        LogUtils.d(TAG, "AI口播特有处理完成: ${task.taskId}")
    }
    

}