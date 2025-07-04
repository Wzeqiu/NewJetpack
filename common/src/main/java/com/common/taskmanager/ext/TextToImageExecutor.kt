package com.common.taskmanager.ext

import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.impl.AbstractTaskExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 文生图任务执行器
 * 专门处理AITaskInfo类型的文生图任务
 */
class TextToImageExecutor : AbstractTaskExecutor<AITaskInfo>() {

    override suspend fun doExecute(task: AITaskInfo) {
        LogUtils.d(TAG, "开始执行文生图任务: ${task.taskId}")

        // 模拟任务执行过程
        for (progress in 0..100 step 5) {
            // 检查是否被取消
            if (!isActive) {
                LogUtils.d(TAG, "文生图任务被取消: ${task.taskId}")
                return
            }

            // 延迟一段时间模拟处理
            delay(300)
        }

        // 设置任务结果
        val resultUrl = "https://example.com/generated_image_${System.currentTimeMillis()}.jpg"

        LogUtils.d(TAG, "文生图任务执行成功: ${task.taskId}, 结果: $resultUrl")
        upDataSuccess(task)
    }



    override fun getTaskClass(): Class<AITaskInfo> {
        return AITaskInfo::class.java
    }

}