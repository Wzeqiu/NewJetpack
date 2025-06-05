package com.common.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private const val BASE_URL = "https://your.api.base.url/"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HeaderInterceptor()) // 添加请求头拦截器
//            .addInterceptor(EncryptionInterceptor()) // 添加加解密拦截器
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            // Consider adding a GsonConverterFactory or other converter if needed
            // .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(service: Class<T>): T = retrofit.create(service)
}

fun <T> CoroutineScope.request(
    block:suspend () -> BaseResponse<T>,
    onFailure:  (Int, String?) -> Unit= { _, _ -> },
    onSuccess: (T) -> Unit
) {
    launch(Dispatchers.IO) {
        runCatching {
            block.invoke()
        }.onSuccess {
            if (it.code==200) {
                onSuccess.invoke(it.data)
            } else {
                onFailure.invoke(it.code, it.message)
            }
        }.onFailure {
           onFailure.invoke(-1, it.message)
        }
    }
}

val httpServer by lazy { RetrofitClient.createService(ApiServer::class.java) }