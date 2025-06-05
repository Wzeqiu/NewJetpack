package com.common.taskmanager.executor

import com.blankj.utilcode.util.ConvertUtils
import com.blankj.utilcode.util.LogUtils
import com.common.db.dao.AITaskInfo
import com.common.network.httpServer
import com.common.taskmanager.core.TaskType
import com.common.taskmanager.helper.FileDownloadHelper
import com.common.taskmanager.helper.NetworkHelper
import com.common.taskmanager.listener.TaskListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 视频类任务执行器（图生视频和AI口播）基类
 */
abstract class VideoTaskExecutor : TaskExecutor() {
    private val mutex = Mutex()
    private val runningJobs = ConcurrentHashMap<String, Job>()
    
    override suspend fun executeTask(task: AITaskInfo, statusListener: TaskListener) {
        // 已删除的任务不执行
        if (task.status == TaskType.TASK_STATUS_DELETE) {
            LogUtils.w(TAG, "任务已删除，不执行: ${task.taskId}")
            return
        }
        
        // 检查任务类型
        if (!isSupportedTaskType(task.type)) {
            LogUtils.e(TAG, "不支持的任务类型: ${task.type}")
            handleTaskFailure(task, statusListener)
            return
        }
        
        // 创建并启动任务作业
        mutex.withLock {
            if (runningJobs.containsKey(task.taskId)) {
                LogUtils.w(TAG, "任务已在执行中: ${task.taskId}")
                return
            }
            
            val job = launch {
                try {
                    processTask(task, statusListener)
                } catch (e: CancellationException) {
                    LogUtils.w(TAG, "任务已取消: ${task.taskId}")
                } catch (e: Exception) {
                    LogUtils.e(TAG, "任务执行异常: ${task.taskId}", e)
                    handleTaskFailure(task, statusListener)
                } finally {
                    mutex.withLock {
                        runningJobs.remove(task.taskId)
                    }
                }
            }
            
            runningJobs[task.taskId] = job
        }
    }
    
    /**
     * 处理任务
     */
    private suspend fun processTask(task: AITaskInfo, statusListener: TaskListener) {
        LogUtils.d(TAG, "开始处理视频任务: ${task.taskId}")
        // 查询任务状态
        NetworkHelper.executeWithRetry {
            delay(2000) // 模拟请求延迟
            httpServer.getTaskInfo(task.taskId)
        }.onSuccess { response ->
            if (response.status==0) {
                        // 任务成功，下载结果文件
                        handleTaskSuccess(task, response.result?:"", statusListener)
                        // 任务仍在运行，稍后再查询
            } else {
                // 请求成功但返回错误码，重试
                LogUtils.w(TAG, "任务状态查询返回错误码: ${response}, 任务ID: ${task.taskId}")
                processTask(task, statusListener)
            }
        }.onFailure { error ->
            // 达到最大重试次数仍然失败
            LogUtils.e(TAG, "任务状态查询失败: ${task.taskId}", error)
            handleTaskFailure(task, statusListener)
        }
    }
    
    /**
     * 处理任务成功
     */
    private suspend fun handleTaskSuccess(task: AITaskInfo, resultUrl: String, statusListener: TaskListener) {
        LogUtils.d(TAG, "视频任务执行成功，开始下载结果: ${task.taskId}")
        
        val targetFile = FileDownloadHelper.createResultFile(task.type)
        FileDownloadHelper.downloadFile(
            resultUrl, 
            targetFile
        ).onSuccess { file ->
            // 获取视频信息

            // 任务特定处理（子类可覆盖）
            handleTaskSpecificSuccess(task)
            
            // 通知状态更新
            statusListener.onTaskStatusChanged(task)

            LogUtils.d(TAG, "视频任务完成: ${task.taskId}")
        }.onFailure { error ->
            LogUtils.e(TAG, "文件下载失败: ${task.taskId}", error)
            handleTaskFailure(task, statusListener)
        }
    }
    
    /**
     * 任务特定成功处理（子类可覆盖）
     */
    protected open fun handleTaskSpecificSuccess(task: AITaskInfo) {
        // 默认空实现，子类可根据需要覆盖
    }
    
    /**
     * 获取视频信息
     */

    /**
     * 处理任务失败
     */
    protected open fun handleTaskFailure(task: AITaskInfo, statusListener: TaskListener) {
        task.status = TaskType.TASK_STATUS_FAILURE
        statusListener.onTaskStatusChanged(task)
        LogUtils.e(TAG, "视频任务失败: ${task.taskId}")
    }
    
    override suspend fun cancelTask(task: AITaskInfo): Boolean {
        return mutex.withLock {
            val job = runningJobs[task.taskId]
            if (job != null && job.isActive) {
                job.cancel()
                runningJobs.remove(task.taskId)
                LogUtils.d(TAG, "任务已取消: ${task.taskId}")
                true
            } else {
                LogUtils.w(TAG, "任务不存在或已完成，无法取消: ${task.taskId}")
                false
            }
        }
    }
    
}