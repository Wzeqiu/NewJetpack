package com.common.network

import com.common.db.dao.AITaskInfo
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiServer {
    @POST("v5/aigc-task/create.html")
    suspend fun getTaskInfo(@Query("taskId") taskId: String): BaseResponse<AITaskInfo>
}