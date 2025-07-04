package com.common.db.dao

import com.common.taskmanager.TaskConstant
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import java.io.Serializable


@Entity
class AITaskInfo : Serializable {
    @Id
    var id: Long?=null

    /**
     * 任务类型
     */
    @TaskConstant.Type
    val type = 0

    /**
     * 任务状态
     */
    @TaskConstant.Status
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
     val read = false

    /**
     * 名称
     */
     val name: String? = null


    /**
     * 如果为true 放到回收站，否者放到视频草稿
     */
    val deleted = false


}
