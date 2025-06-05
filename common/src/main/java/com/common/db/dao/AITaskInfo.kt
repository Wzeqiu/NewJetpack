package com.common.db.dao

import com.common.taskmanager.core.TaskType
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import java.io.Serializable


@Entity
class AITaskInfo : Serializable {
    @Id
    val id: Long = 0

    /**
     * 任务类型
     */
    @TaskType.Type
    val type = 0

    /**
     * 任务状态
     */
    @TaskType.Status
    var status = 0

    /**
     * 任务id
     */
    val taskId: String=""

    /**
     * 创建时间
     */
    val createTime = System.currentTimeMillis()

    /**
     * 封面
     */
    val cover: String? = null

    /**
     * 结果
     */
    var result: String? = null

    /**
     * 用户Id
     */
    val userId = ""

    /**
     * 模板信息结果大小
     */
    var size: String? = null

    /**
     * 预计结束时间
     */
    val avgFinishTime = 0

    /**
     * 时长
     */
    val duration: Long = 0

    /**
     * 是否查看结果
     */
    private val read = false

    /**
     * 名称
     */
    private val name: String? = null


    /**
     * 如果为true 放到回收站，否者放到视频草稿
     */
    val deleted = false


}
