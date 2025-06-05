package com.common.taskmanager.helper

import com.blankj.utilcode.util.LogUtils
import com.common.network.BaseResponse
import kotlinx.coroutines.delay

/**
 * 网络操作辅助类
 */
object NetworkHelper {
    private const val TAG = "NetworkHelper"

    /**
     * 执行网络请求并支持重试
     *
     * @param maxRetries 最大重试次数
     * @param initialDelay 初始延迟时间(毫秒)
     * @param maxDelay 最大延迟时间(毫秒)
     * @param factor 延迟时间增长因子
     * @param block 要执行的网络请求代码块
     * @return 网络请求结果
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 15000,
        factor: Double = 2.0,
        block: suspend () -> BaseResponse<T>
    ): Result<T> {
        var currentDelay = initialDelay
        var lastError: Throwable? = null

        repeat(maxRetries) { attempt ->
            // 执行网络请求
            runCatching {
                block()
            }.onSuccess {
                if (it.isSuccess()){
                    return Result.success(it.data)
                }
            }
            try {
                val result = block()
                if (result.isSuccess()) {
                    return result
                }
                lastError = result.exceptionOrNull()
            } catch (e: Exception) {
                lastError = e
            }

            // 达到最大重试次数，返回失败
            if (attempt == maxRetries - 1) {
                return Result.failure(lastError ?: Exception("未知错误"))
            }

            // 记录重试日志
            LogUtils.w(TAG, "请求失败，准备第${attempt + 1}次重试，延迟${currentDelay}ms", lastError)

            // 等待一段时间后重试
            delay(currentDelay)

            // 增加延迟时间，但不超过最大延迟
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }

        return Result.failure(lastError ?: Exception("未知错误"))
    }
}