package com.common.taskmanager.executor

import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.network.httpServer
import com.common.taskmanager.core.TaskType
import kotlinx.coroutines.delay

/**
 * 图生视频任务执行器
 */
class PictureToVideoExecutor : VideoTaskExecutor() {
    companion object {
        private const val MAX_PROGRESS_CHECK_RETRIES = 30 // 最大进度检查重试次数
        private const val PROGRESS_CHECK_INTERVAL = 3000L // 进度检查间隔时间(毫秒)
    }

    override fun isSupportedTaskType(taskType: Int): Boolean {
        return taskType == TaskType.AI_TYPE_PICTURE_TO_VIDEO
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(TaskType.AI_TYPE_PICTURE_TO_VIDEO)
    }
    
    /**
     * 处理任务特定成功
     * 在父类成功处理基础上，添加图生视频特有的处理逻辑
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
        
        LogUtils.d(TAG, "图生视频特有处理完成: ${task.taskId}")
    }
    
}