package com.common.network

import com.common.db.dao.AITaskInfo
import retrofit2.http.POST

interface ApiServer {
    @POST("v5/aigc-task/create.html")
    fun getTaskInfo(taskId: String): BaseResponse<AITaskInfo>
}