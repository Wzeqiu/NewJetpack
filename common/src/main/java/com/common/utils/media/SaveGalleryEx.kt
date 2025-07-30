package com.common.utils.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.Utils
import com.common.common.R
import com.common.utils.media.FileMimeDetector.getMimeType
import com.common.utils.media.FileMimeDetector.isImage
import com.common.utils.media.FileMimeDetector.isVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

/**
 * 批量保存到相册
 */
fun AppCompatActivity.saveToAlbum(vararg paths: String): List<String> {
    if (paths.isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    paths.map { File(it) }.forEach { file ->
        when {
            file.isImage() -> result.add(saveImageToGallery(file))
            file.isVideo() -> result.add(saveVideoToGallery(file))
            else -> Log.w("SaveToAlbum", "不支持的文件类型: $file")
        }
    }
    ToastUtils.showShort("已保存到相册")
    return result
}

/**
 * 批量保存到相册
 */
fun AppCompatActivity.saveToAlbum(vararg paths: String, callback: (List<String>) -> Unit = {}) {
    if (paths.isEmpty()) return callback.invoke(emptyList())
    val result = mutableListOf<String>()
    lifecycleScope.launch(Dispatchers.IO) {
        paths.map { File(it) }.forEach { file ->
            when {
                file.isImage() -> result.add(saveImageToGallery(file))
                file.isVideo() -> result.add(saveVideoToGallery(file))
                else -> Log.w("SaveToAlbum", "不支持的文件类型: $file")
            }
        }
        withContext(Dispatchers.Main) {
            ToastUtils.showShort("已保存到相册")
            callback.invoke(result)
        }
    }
}

/**
 * 保存图片到相册
 */
private fun Context.saveImageToGallery(file: File): String {
    return runCatching {
        val mime = file.getMimeType() ?: ""
        val fileName = FileUtils.getFileNameNoExtension(file)
        val values = ContentValues().apply {
            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "${System.currentTimeMillis()}_${Random.nextInt(1000000)}_$fileName"
            )
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/${
                        Utils.getApp().getString(R.string.app_name)
                    }/"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val bitmap = ImageUtils.getBitmap(file)
                val format = when (mime.lowercase()) {
                    "image/png" -> Bitmap.CompressFormat.PNG
                    "image/webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }

                bitmap.compress(format, 100, outputStream)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }
                val filePath = UriUtils.uri2File(uri).absolutePath
                notifyMediaScan(filePath, mime)
                filePath
            }
        } ?: ""
    }.onFailure {
        Log.e("SaveToAlbum", "保存图片失败", it)
        ToastUtils.showShort("保存图片失败: ${it.message}")
    }.getOrDefault("")
}

/**
 * 保存视频到相册
 */
private fun Context.saveVideoToGallery(file: File): String {
    return runCatching {
        val mime = file.getMimeType() ?: ""
        val fileName = FileUtils.getFileNameNoExtension(file)
        val values = ContentValues().apply {
            put(
                MediaStore.Video.Media.DISPLAY_NAME,
                "${System.currentTimeMillis()}_${Random.nextInt(1000000)}_$fileName"
            )
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/${
                        Utils.getApp().getString(R.string.app_name)
                    }/"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                file.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                    val filePath = UriUtils.uri2File(uri).absolutePath
                    notifyMediaScan(filePath, mime)
                    filePath
                }
            }
        } ?: ""
    }.onFailure {
        Log.e("SaveToAlbum", "保存视频失败", it)
        ToastUtils.showShort("保存视频失败: ${it.message}")
    }.getOrDefault("")
}

/**
 * 通知媒体库扫描新文件
 */
private fun Context.notifyMediaScan(filePath: String, mimeType: String) {
    try {
        MediaScannerConnection.scanFile(
            this,
            arrayOf(filePath),
            arrayOf(mimeType)
        ) { _, scanUri ->
            Log.d("MediaScanner", "文件已扫描: $filePath -> $scanUri")
        }
    } catch (e: Exception) {
        Log.e("MediaScanner", "媒体扫描失败", e)
    }
}

