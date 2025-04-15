package com.common.net

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


private val httpRetrofit by lazy { getRetrofit() }


fun <T> createServer(server: Class<T>): T {
    return httpRetrofit.create(server)
}


fun getRetrofit(): Retrofit {
    return Retrofit.Builder()
        .baseUrl("")
        .client(getOkHttpClient())
        .build()
}

fun getOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor())
        .build()
}