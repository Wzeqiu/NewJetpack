package com.common.network

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
            .addInterceptor(dynamicTimeoutInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 创建 DynamicTimeoutInterceptor 实例
    private val dynamicTimeoutInterceptor = DynamicTimeoutInterceptor()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // 将动态超时拦截器添加到 OkHttpClient
            // Consider adding a GsonConverterFactory or other converter if needed
            // .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * 创建 Retrofit 服务，支持动态域名切换和超时配置
     * @param service 服务接口类
     * @param domains 备用域名列表，每个元素包含域名和对应的连接、读取、写入超时时间（秒）
     * @return 服务接口的实例
     */
    fun <T> createService(service: Class<T>, domains: List<Pair<String, Triple<Long, Long, Long>>>): T {
        // 设置动态超时拦截器的域名列表和超时时间
        dynamicTimeoutInterceptor.setDomains(domains)
        // 使用默认的 retrofit 实例，拦截器会处理域名切换和超时
        return retrofit.create(service)
    }
}