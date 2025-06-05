package com.common.taskmanager.helper

import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * 重试辅助类
 * 实现指数退避策略(exponential backoff)，使重试更加健壮
 */
class RetryHelper {
    companion object {
        private const val TAG = "RetryHelper"
        
        /**
         * 使用指数退避策略执行带重试的操作
         * 
         * @param maxRetries 最大重试次数
         * @param initialDelayMs 初始延迟时间(毫秒)
         * @param maxDelayMs 最大延迟时间(毫秒)
         * @param factor 退避因子，决定延迟增长速度
         * @param jitter 是否添加随机抖动以避免多个客户端同时重试
         * @param retryOnException 指定哪些异常类型需要重试
         * @param block 要执行的操作
         * @return 操作结果
         */
        suspend fun <T> retryWithExponentialBackoff(
            maxRetries: Int = 3,
            initialDelayMs: Long = 1000,
            maxDelayMs: Long = 60000,
            factor: Double = 2.0,
            jitter: Boolean = true,
            retryOnException: (Throwable) -> Boolean = { true },
            block: suspend () -> T
        ): T {
            var currentDelay = initialDelayMs
            var attempt = 0
            
            while (true) {
                try {
                    return block()
                } catch (e: Exception) {
                    attempt++
                    
                    // 判断是否需要对此异常进行重试
                    if (!retryOnException(e)) {
                        LogUtils.w(TAG, "异常不符合重试条件，直接抛出: ${e.message}")
                        throw e
                    }
                    
                    // 达到最大重试次数，抛出最后一个异常
                    if (attempt >= maxRetries) {
                        LogUtils.e(TAG, "达到最大重试次数($maxRetries)，操作失败: ${e.message}")
                        throw e
                    }
                    
                    // 计算下一次重试的延迟时间
                    currentDelay = calculateNextDelay(currentDelay, attempt, factor, maxDelayMs, jitter)
                    
                    LogUtils.w(TAG, "操作失败，将在${currentDelay}ms后重试(${attempt}/${maxRetries}): ${e.message}")
                    delay(currentDelay)
                }
            }
        }
        
        /**
         * 计算下一次重试的延迟时间
         */
        private fun calculateNextDelay(
            currentDelay: Long,
            attempt: Int,
            factor: Double,
            maxDelayMs: Long,
            jitter: Boolean
        ): Long {
            // 使用指数退避公式: initialDelay * (factor ^ attempt)
            var nextDelay = (currentDelay * factor.pow(attempt)).toLong()
            
            // 限制最大延迟时间
            nextDelay = min(nextDelay, maxDelayMs)
            
            // 添加随机抖动(0.5-1.5倍)，避免多个客户端同时重试
            if (jitter) {
                val jitterFactor = 0.5 + Random.nextDouble()
                nextDelay = (nextDelay * jitterFactor).toLong()
            }
            
            return nextDelay
        }
        
        /**
         * 重试网络请求
         * 针对网络请求的特定重试策略
         */
        suspend fun <T> retryNetworkRequest(
            maxRetries: Int = 3,
            initialDelayMs: Long = 2000,
            block: suspend () -> T
        ): T {
            return retryWithExponentialBackoff(
                maxRetries = maxRetries,
                initialDelayMs = initialDelayMs,
                maxDelayMs = 20000,
                factor = 1.5,
                jitter = true,
                retryOnException = { e ->
                    // 只对网络相关异常进行重试
                    e is java.io.IOException || 
                    e is java.net.SocketTimeoutException ||
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("connection", ignoreCase = true) == true
                },
                block = block
            )
        }
        
        /**
         * 重试文件操作
         * 针对文件操作的特定重试策略
         */
        suspend fun <T> retryFileOperation(
            maxRetries: Int = 3,
            initialDelayMs: Long = 1000,
            block: suspend () -> T
        ): T {
            return retryWithExponentialBackoff(
                maxRetries = maxRetries,
                initialDelayMs = initialDelayMs,
                maxDelayMs = 10000,
                factor = 2.0,
                jitter = false,
                retryOnException = { e ->
                    // 只对文件访问相关异常进行重试
                    e is java.io.FileNotFoundException ||
                    e is java.io.IOException ||
                    e.message?.contains("file", ignoreCase = true) == true ||
                    e.message?.contains("access", ignoreCase = true) == true ||
                    e.message?.contains("permission", ignoreCase = true) == true
                },
                block = block
            )
        }
        
        /**
         * 重试API调用
         * 针对API调用的特定重试策略
         */
        suspend fun <T> retryApiCall(
            maxRetries: Int = 3,
            initialDelayMs: Long = 3000,
            shouldRetry: (T) -> Boolean = { false },
            block: suspend () -> T
        ): T {
            var result: T
            var currentDelay = initialDelayMs
            
            for (attempt in 0 until maxRetries) {
                result = block()
                
                // 检查返回结果是否表明需要重试
                if (!shouldRetry(result)) {
                    return result
                }
                
                // 计算下一次重试的延迟时间
                currentDelay = calculateNextDelay(
                    currentDelay = currentDelay,
                    attempt = attempt,
                    factor = 2.0,
                    maxDelayMs = 30000,
                    jitter = true
                )
                
                LogUtils.w(TAG, "API返回需要重试的结果，将在${currentDelay}ms后重试(${attempt+1}/${maxRetries})")
                delay(currentDelay)
            }
            
            // 达到最大重试次数，返回最后一次结果
            return block()
        }
    }
} 