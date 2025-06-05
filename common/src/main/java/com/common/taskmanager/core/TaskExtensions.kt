package com.common.taskmanager.core

import com.blankj.utilcode.constant.TimeConstants
import com.common.db.dao.AITaskInfo

/**
 * 任务扩展函数
 * 提供对AITaskInfo对象的便捷操作
 */

/**
 * 任务是否处于创建状态
 */
fun AITaskInfo.isCreate(): Boolean = status == TaskType.TASK_STATUS_CREATE

/**
 * 任务是否处于创建状态且在最近一小时内创建
 */
fun AITaskInfo.isCreateInHour(): Boolean =
    status == TaskType.TASK_STATUS_CREATE && (createTime + TimeConstants.HOUR) > System.currentTimeMillis()

/**
 * 任务是否已成功完成
 */
fun AITaskInfo.isSuccess(): Boolean = status == TaskType.TASK_STATUS_SUCCESS

/**
 * 任务是否已失败
 */
fun AITaskInfo.isFailure(): Boolean = status == TaskType.TASK_STATUS_FAILURE

/**
 * 任务是否已删除
 */
fun AITaskInfo.isDelete(): Boolean = status == TaskType.TASK_STATUS_DELETE

/**
 * 获取任务类型的显示名称
 */
fun AITaskInfo.getTaskTypeName(): String {
    return when (type) {
        TaskType.AI_TYPE_ORAL_BROADCASTING -> "AI口播"
        TaskType.AI_TYPE_PICTURE_TO_VIDEO -> "图生视频"
        TaskType.AI_TYPE_TEXT_TO_IMAGE -> "文生图"
        else -> "未知任务"
    }
}

/**
 * 获取任务状态的显示名称
 */
fun AITaskInfo.getStatusName(): String {
    return when (status) {
        TaskType.TASK_STATUS_CREATE -> "处理中"
        TaskType.TASK_STATUS_SUCCESS -> "已完成"
        TaskType.TASK_STATUS_FAILURE -> "失败"
        TaskType.TASK_STATUS_DELETE -> "已删除"
        else -> "未知状态"
    }
} 