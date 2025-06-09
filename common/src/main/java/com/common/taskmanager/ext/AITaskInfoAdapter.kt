package com.common.taskmanager.ext

import com.common.db.DbManager
import com.common.db.dao.AITaskInfo
import com.common.taskmanager.TaskConstant
import com.common.taskmanager.api.TaskAdapter

/**
 * AITaskInfo适配器
 * 将AITaskInfo适配为统一的任务接口
 */
class AITaskInfoAdapter : TaskAdapter<AITaskInfo> {
    
    override fun getTaskId(task: AITaskInfo): String = task.taskId
    
    override fun getType(task: AITaskInfo): Int = task.type
    
    override fun getStatus(task: AITaskInfo): Int = task.status
    
    override suspend fun saveTask(task: AITaskInfo) {
        DbManager.taskInfoDao.insert(task)
    }
    
    override suspend fun updateTask(task: AITaskInfo) {
        DbManager.taskInfoDao.update(task)
    }
    
    override suspend fun deleteTask(task: Any) {
        if (task is AITaskInfo) {
            DbManager.taskInfoDao.delete(task)
        } else if (task is List<*>) {
            val tasks = task.filterIsInstance<AITaskInfo>()
            tasks.forEach { DbManager.taskInfoDao.delete(it) }
        }
    }
    
    override suspend fun loadAllTasks(): List<AITaskInfo> {
        return DbManager.taskInfoDao.getAll()
    }
    
    override suspend fun findTask(taskId: String): AITaskInfo? {
        return DbManager.taskInfoDao.getAll().find { it.taskId == taskId }
    }
    
    override fun setStatus(task: AITaskInfo, status: Int) {
        task.status = status
    }
    
    override fun setResult(task: AITaskInfo, result: String?) {
        task.result = result
    }
    
    override fun setProgress(task: AITaskInfo, progress: Int) {
        // AITaskInfo 没有直接的progress字段，可以在其他字段中存储进度信息
        // 也可以通过扩展属性或自定义字段来实现
    }
    
    override fun markStarted(task: AITaskInfo) {
        task.status = TaskConstant.TASK_STATUS_RUNNING
    }
    
    override fun markSuccess(task: AITaskInfo, result: String?) {
        task.status = TaskConstant.TASK_STATUS_SUCCESS
        task.result = result
    }
    
    override fun markFailure(task: AITaskInfo, reason: String?) {
        task.status = TaskConstant.TASK_STATUS_FAILURE
        // 可以在result字段中存储失败原因
        if (reason != null) {
            task.result = "失败原因：$reason"
        }
    }
    
    override fun getTaskClass(): Class<AITaskInfo> {
        return AITaskInfo::class.java
    }
} 