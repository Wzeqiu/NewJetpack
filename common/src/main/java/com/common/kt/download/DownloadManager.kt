package com.common.kt.download

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

private val retrofitBuilder by lazy {
    Retrofit.Builder().baseUrl("https://www.baidu.com").client(
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
                    lifecycleScope.launch(Dispatchers.Main) {
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
                withContext(Dispatchers.Main) { downloadListener.onDownloadStart() }
                val responseBody = downloadServer.downloadFile(url)
                val contentLength = responseBody.contentLength()
                val readBuffer = ByteArray(1024 * 1024 * 2)
                RandomAccessFile(targetFile, "rwd").use { randomAccessFile ->
                    randomAccessFile.setLength(contentLength)
                    responseBody.source().use { source ->
                        var readLength: Int
                        var writeContentLength = 0L
                        while ((source.read(readBuffer).also { readLength = it } != -1)) {
                            randomAccessFile.write(readBuffer, 0, readLength)
                            writeContentLength += readLength
                            withContext(Dispatchers.Main) {
                                downloadListener.onDownloadProgress((writeContentLength * 100 / contentLength).toInt())
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) { downloadListener.onDownloadComplete() }
            }.onFailure {
                targetFile.delete()
                if (it is CancellationException) {
                    withContext(Dispatchers.Main) { downloadListener.onDownloadCancel() }
                } else {
                    withContext(Dispatchers.Main) { downloadListener.onDownloadError(it) }
                }
            }
        }
    }


    interface DownloadListener {
        fun onDownloadStart() {}
        fun onDownloadProgress(progress: Int) {}
        fun onDownloadComplete() {}
        fun onDownloadError(throwable: Throwable) {}
        fun onDownloadCancel() {}
    }
}