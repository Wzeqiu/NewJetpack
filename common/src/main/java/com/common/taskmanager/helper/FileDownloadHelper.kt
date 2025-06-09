package com.common.taskmanager.helper

import android.util.Log
import com.common.taskmanager.TaskConstant
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.random.Random

/**
 * 文件下载辅助类
 */
object FileDownloadHelper {
    private const val TAG = "FileDownloadHelper"
    
    /**
     * 创建任务结果文件
     *
     * @param taskType 任务类型
     * @return 文件对象
     */
    fun createResultFile(@TaskConstant.Type taskType: Int): File {
        val extension = when (taskType) {
            TaskConstant.AI_TYPE_TEXT_TO_IMAGE -> "png"
            else -> "mp4"
        }
        
        val fileName = "result_${System.currentTimeMillis()}_${Random.nextInt(10000)}.$extension"
        return File("", fileName)
    }
    
    /**
     * 下载文件
     *
     * @param url 文件URL
     * @param targetFile 目标文件
     * @return 下载结果
     */
    suspend fun downloadFile(
        url: String,
        targetFile: File
    ): Result<File> {
        return NetworkHelper.executeWithRetry {
            suspendCancellableCoroutine { continuation ->
                Log.d(TAG, "开始下载文件: $url -> ${targetFile.absolutePath}")

                continuation.invokeOnCancellation {
                    Log.d(TAG, "下载已取消: $url")
                }
            }
        }
    }
} 