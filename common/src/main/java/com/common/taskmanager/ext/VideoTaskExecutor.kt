package com.common.taskmanager.ext

import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskCallback
import com.common.taskmanager.impl.AbstractTaskExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 视频任务执行器
 * 处理视频生成相关任务
 * @param adapter 从外部注入的适配器实例，避免重复创建
 */
class VideoTaskExecutor(override val adapter: AITaskInfoAdapter) : AbstractTaskExecutor<AITaskInfo, AITaskInfoAdapter>() {

    override suspend fun doExecute(
        task: AITaskInfo, 
        callback: TaskCallback<AITaskInfo>
    ) {
        LogUtils.d(TAG, "开始执行视频任务: ${task.taskId}")

        // 视频任务进度通常分为多个阶段
        val phases = listOf("初始化", "素材准备", "渲染中", "后期处理", "导出")
        
        // 模拟多阶段处理
        for ((phaseIndex, phaseName) in phases.withIndex()) {
            val startProgress = phaseIndex * 20
            val endProgress = (phaseIndex + 1) * 20
            
            LogUtils.d(TAG, "视频任务阶段: $phaseName")
            
            // 模拟阶段内进度
            for (progress in startProgress..endProgress step 2) {
                // 检查是否被取消
                if (!isActive) {
                    LogUtils.d(TAG, "视频任务被取消: ${task.taskId}")
                    return
                }
                
                // 更新进度
                adapter.setProgress(task, progress)
                callback.onProgressUpdate(task, progress)
                
                // 延迟模拟处理时间
                delay(500)
            }
        }
        
        // 设置任务成功完成
        val videoUrl = "https://example.com/generated_video_${System.currentTimeMillis()}.mp4"
        adapter.markSuccess(task, videoUrl)
        callback.onSuccess(task, videoUrl)
        
        LogUtils.d(TAG, "视频任务执行成功: ${task.taskId}, 结果: $videoUrl")
    }

    override fun isSupportedTaskType(taskType: Int): Boolean {
        return getSupportedTaskTypes().contains(taskType)
    }
    
    override fun getSupportedTaskTypes(): List<Int> {
        return listOf(
            TaskConstant.AI_TYPE_VIDEO_GENERATION,
            TaskConstant.AI_TYPE_VIDEO_EDITING
        )
    }
    
    override fun getTaskClass(): Class<AITaskInfo> {
        return AITaskInfo::class.java
    }
} 