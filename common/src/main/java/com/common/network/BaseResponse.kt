package com.common.network

data class BaseResponse<T>(
    var code: Int = 0,
    var message: String? = null,
    var data: T
){
    fun isSuccess(): Boolean {
        return code == 0
    }
}