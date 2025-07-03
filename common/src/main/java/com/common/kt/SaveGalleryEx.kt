package com.common.kt

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
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
import com.common.kt.activity.requestPermission
import com.common.utils.getMimeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


/**
 * 批量保存到相册
 */
fun AppCompatActivity.saveToAlbum(paths: List<String>) {
    if (paths.isEmpty()) return

    requestPermission(MANAGE_EXTERNAL_STORAGE) {
        paths.forEach { path ->
            when {
                isImageFile(path) -> saveImageToGallery(path)
                isVideoFile(path) -> saveVideoToGallery(path)
                else -> Log.w("SaveToAlbum", "不支持的文件类型: $path")
            }
        }
        ToastUtils.showShort("已保存到相册")
    }
}

/**
 * 判断是否为图片文件
 */
private fun isImageFile(path: String): Boolean {
    val extension = FileUtils.getFileExtension(path).lowercase()
    return extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
}

/**
 * 判断是否为视频文件
 */
private fun isVideoFile(path: String): Boolean {
    val extension = FileUtils.getFileExtension(path).lowercase()
    return extension in listOf("mp4", "3gp", "mkv", "webm", "avi", "mov")
}

/**
 * 保存图片到相册
 */
private fun Context.saveImageToGallery(path: String) {
    try {
        val mime = getMimeType(FileUtils.getFileExtension(path))
        val fileName = FileUtils.getFileNameNoExtension(path)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/${Utils.getApp().getString(R.string.app_name)}/"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val bitmap = ImageUtils.getBitmap(path)
                val format = when (FileUtils.getFileExtension(path).lowercase()) {
                    "png" -> Bitmap.CompressFormat.PNG
                    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }

                bitmap.compress(format, 100, outputStream)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }

                notifyMediaScan(uri, mime)
            }
        }
    } catch (e: Exception) {
        Log.e("SaveToAlbum", "保存图片失败", e)
        ToastUtils.showShort("保存图片失败: ${e.message}")
    }
}

/**
 * 保存视频到相册
 */
private fun Context.saveVideoToGallery(path: String) {
    try {
        val mime = getMimeType(FileUtils.getFileExtension(path))
        val fileName = FileUtils.getFileNameNoExtension(path)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/${Utils.getApp().getString(R.string.app_name)}/"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                File(path).inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }

                    notifyMediaScan(uri, mime)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("SaveToAlbum", "保存视频失败", e)
        ToastUtils.showShort("保存视频失败: ${e.message}")
    }
}

/**
 * 通知媒体库扫描新文件
 */
private fun Context.notifyMediaScan(uri: Uri, mimeType: String) {
    try {
        val filePath = UriUtils.uri2File(uri).absolutePath
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


/**
 * 异步批量保存到相册（协程版本）
 */
fun AppCompatActivity.saveToAlbumAsync(paths: List<String>, onComplete: (Int) -> Unit = {}) {
    if (paths.isEmpty()) {
        onComplete(0)
        return
    }

    requestPermission(MANAGE_EXTERNAL_STORAGE) {
        lifecycleScope.launch(Dispatchers.Main) {
            var successCount = 0

            paths.forEach { path ->
                val success = when {
                    isImageFile(path) -> withContext(Dispatchers.IO) { saveImageToGalleryAsync(path) }
                    isVideoFile(path) -> withContext(Dispatchers.IO) { saveVideoToGalleryAsync(path) }
                    else -> false
                }
                if (success) successCount++
            }

            if (successCount > 0) {
                ToastUtils.showShort("成功保存 $successCount 个文件")
            }
            onComplete(successCount)
        }
    }
}

/**
 * 异步保存图片到相册
 */
private suspend fun Context.saveImageToGalleryAsync(path: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val mime = getMimeType(FileUtils.getFileExtension(path))
        val fileName = FileUtils.getFileNameNoExtension(path)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/${Utils.getApp().getString(R.string.app_name)}/"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val bitmap = ImageUtils.getBitmap(path)
                val format = when (FileUtils.getFileExtension(path).lowercase()) {
                    "png" -> Bitmap.CompressFormat.PNG
                    "webp" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }

                bitmap.compress(format, 100, outputStream)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, values, null, null)
                }

                notifyMediaScanAsync(uri, mime)
                return@withContext true
            }
        }
        false
    } catch (e: Exception) {
        Log.e("SaveToAlbum", "异步保存图片失败", e)
        false
    }
}

/**
 * 异步保存视频到相册
 */
private suspend fun Context.saveVideoToGalleryAsync(path: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val mime = getMimeType(FileUtils.getFileExtension(path))
        val fileName = FileUtils.getFileNameNoExtension(path)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/${Utils.getApp().getString(R.string.app_name)}/"
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                File(path).inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream, DEFAULT_BUFFER_SIZE)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear()
                        values.put(MediaStore.Video.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }

                    notifyMediaScanAsync(uri, mime)
                    return@withContext true
                }
            }
        }
        false
    } catch (e: Exception) {
        Log.e("SaveToAlbum", "异步保存视频失败", e)
        false
    }
}

/**
 * 异步通知媒体库扫描新文件
 */
private suspend fun Context.notifyMediaScanAsync(uri: Uri, mimeType: String) = withContext(Dispatchers.IO) {
    try {
        val filePath = UriUtils.uri2File(uri).absolutePath
        MediaScannerConnection.scanFile(
            this@notifyMediaScanAsync,
            arrayOf(filePath),
            arrayOf(mimeType)
        ) { _, scanUri ->
            Log.d("MediaScanner", "文件已扫描: $filePath -> $scanUri")
        }
    } catch (e: Exception) {
        Log.e("MediaScanner", "媒体扫描失败", e)
    }
}
