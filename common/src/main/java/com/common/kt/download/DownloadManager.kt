package com.common.kt.download

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

private val retrofitBuilder by lazy {
    Retrofit.Builder().baseUrl("https://www.alibabagroup.com").client(
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS).build()
    ).build()
}

private val downloadServer by lazy { retrofitBuilder.create(DownloadService::class.java) }

object DownloadManager {
    fun LifecycleOwner.bindLifeDownloadFile(
        url: String, targetFile: File, downloadListener: DownloadListener
    ) {
        var downloadJob: Job? = null
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                lifecycle.removeObserver(this)
                downloadJob?.cancel()
            }
        }
        lifecycleScope.launch {
            downloadJob = downloadFile(url, targetFile, downloadListener).also {
                it.invokeOnCompletion {
                    launch(Dispatchers.Main) {
                        lifecycle.removeObserver(lifecycleObserver)
                    }
                }
            }
        }
        lifecycle.addObserver(lifecycleObserver)
    }

    fun downloadFile(url: String, targetFile: File, downloadListener: DownloadListener): Job {
        return download(url, targetFile, downloadListener)
    }


    @OptIn(DelicateCoroutinesApi::class)
    private fun download(url: String, targetFile: File, downloadListener: DownloadListener): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                downloadListener.onDownloadStart()
                val responseBody = downloadServer.downloadFile(url)
                val contentLength = responseBody.contentLength()
                val readBuffer = ByteArray(1024 * 1024 * 4)
                RandomAccessFile(targetFile, "rwd").use { randomAccessFile ->
                    randomAccessFile.setLength(contentLength)
                    responseBody.source().use { source ->
                        var readLength: Int
                        var writeContentLength = 0L
                        while ((source.read(readBuffer).also { readLength = it } != -1)) {
                            randomAccessFile.write(readBuffer, 0, readLength)
                            writeContentLength += readLength
                            downloadListener.onDownloadProgress((writeContentLength * 100 / contentLength).toInt())
                        }
                    }
                }
                downloadListener.onDownloadComplete(targetFile)
            }.onFailure {
                targetFile.delete()
                if (it is CancellationException) {
                    downloadListener.onDownloadCancel()
                } else {
                    downloadListener.onDownloadError(it)
                }
            }
        }
    }


    interface DownloadListener {
        fun onDownloadStart() {}
        fun onDownloadProgress(progress: Int) {}
        fun onDownloadComplete(targetFile: File) {}
        fun onDownloadError(throwable: Throwable) {}
        fun onDownloadCancel() {}
    }
}