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

    override fun saveTask(task: AITaskInfo) {
        DbManager.taskInfoDao.insert(task)
    }

    override fun updateTask(task: AITaskInfo) {
        DbManager.taskInfoDao.update(task)
    }

    override fun deleteTask(task: Any) {
        if (task is AITaskInfo) {
            DbManager.taskInfoDao.delete(task)
        } else if (task is List<*>) {
            val tasks = task.filterIsInstance<AITaskInfo>()
            tasks.forEach { DbManager.taskInfoDao.delete(it) }
        }
    }

    override fun loadAllTasks(): List<AITaskInfo> {
        return DbManager.taskInfoDao.getAll()
    }

    override fun findTask(taskId: String): AITaskInfo? {
        return DbManager.taskInfoDao.getAll().find { it.taskId == taskId }
    }


    override fun setProgress(task: AITaskInfo, progress: Int) {
        // AITaskInfo 没有直接的progress字段，可以在其他字段中存储进度信息
        // 也可以通过扩展属性或自定义字段来实现
    }

    override fun markStarted(task: AITaskInfo) {
        // 开始制作
        task.status = TaskConstant.TASK_STATUS_RUNNING
    }

    override fun markSuccess(task: AITaskInfo) {
        //制作成功
        task.status = TaskConstant.TASK_STATUS_SUCCESS
    }

    override fun markFailure(task: AITaskInfo) {
        // 制作失败
        task.status = TaskConstant.TASK_STATUS_FAILURE
    }

    override fun markDelete(task: AITaskInfo) {
        // 删除任务
        task.status = TaskConstant.TASK_STATUS_DELETE
    }


    override fun getTaskClass(): Class<AITaskInfo> {
        return AITaskInfo::class.java
    }
} 