package com.common.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 添加请求头拦截器
 */
class HeaderInterceptor  : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            // Add headers here, e.g.:
            // .header("Authorization", "Bearer your_token")
            // .header("X-Custom-Header", "value")
            .build()
        return chain.proceed(newRequest)
    }
}