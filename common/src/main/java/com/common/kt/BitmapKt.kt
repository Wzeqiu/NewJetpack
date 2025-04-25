package com.common.kt

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.Utils
import com.common.common.R
import com.common.kt.activity.requestPermission
import com.common.utils.PathManager
import com.common.utils.getMimeType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.random.Random


private const val maxImageWidth = 1080
private const val maxImageHeight = 1080


/**
 * 压缩图片
 */
suspend fun getCompressImagePath(path: String): String? {
    return suspendCancellableCoroutine {
        val degree = ImageUtils.getRotateDegree(path)
        val bitmap = ImageUtils.getBitmap(path, maxImageWidth, maxImageHeight)?.let { bitmap ->
            if (degree > 0) {
                ImageUtils.rotate(bitmap, degree, bitmap.width / 2f, bitmap.height / 2f, true)
            } else {
                bitmap
            }
        } ?: return@suspendCancellableCoroutine it.resume(null)
        val newCachePath =
            PathManager.CACHE_IMAGE + File.separator + "${System.currentTimeMillis()}${
                Random.nextInt(
                    1000000
                )
            }.jpg"
        if (ImageUtils.save(bitmap, newCachePath, Bitmap.CompressFormat.JPEG, true)) {
            it.resume(newCachePath)
        } else {
            FileUtils.delete(newCachePath)
            it.resume(null)
        }
    }
}
/**
 * 压缩图片
 */
suspend fun getCompressImageBitmap(path: String): Pair<String, Bitmap>? {
    return suspendCancellableCoroutine {
        val degree = ImageUtils.getRotateDegree(path)
        val bitmap = ImageUtils.getBitmap(path, maxImageWidth, maxImageHeight)?.let { bitmap ->
            if (degree > 0) {
                ImageUtils.rotate(bitmap, degree, bitmap.width / 2f, bitmap.height / 2f, true)
            } else {
                bitmap
            }
        } ?: return@suspendCancellableCoroutine it.resume(null)
        val newCachePath =
            PathManager.CACHE_IMAGE + File.separator + "${System.currentTimeMillis()}${
                Random.nextInt(
                    100000
                )
            }.jpg"
        if (ImageUtils.save(bitmap, newCachePath, Bitmap.CompressFormat.JPEG)) {
            it.resume(newCachePath to bitmap)
        } else {
            FileUtils.delete(newCachePath)
            it.resume(null)
        }
    }
}

private val detector by lazy { FaceDetection.getClient(FaceDetectorOptions.Builder().build()) }
suspend fun imageFaceDetect(bitmap: Bitmap): List<Face>? {
    return suspendCancellableCoroutine { block ->
        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener {
                if (it.isNotEmpty()) {
                    block.resume(it)
                } else {
                    block.resume(null)
                }
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }.addOnFailureListener {
                block.resume(null)
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
    }
}

suspend fun checkImageFace(path: String, block: (Boolean, String) -> Unit) {
    getCompressImageBitmap(path)?.let { compress ->
        imageFaceDetect(compress.second)?.let { faces ->
            block(true, compress.first)
        } ?: block(false, "")
    } ?: block(false, "")
}

/**
 * 保存到相册
 */
fun AppCompatActivity.saveToAlbum(path: String) {
    requestPermission(MANAGE_EXTERNAL_STORAGE) {
        saveVideoToGallery(path)
    }
}

private fun Context.saveImageToGallery(path: String) {
    val mime = getMimeType(FileUtils.getFileExtension(path))
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, FileUtils.getFileNameNoExtension(path))
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/${Utils.getApp().getString(R.string.app_name)}/"
            )
        }
    }
    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let {
        contentResolver.openOutputStream(it)?.use { outputStream ->
            ImageUtils.getBitmap(path).compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            MediaScannerConnection.scanFile(
                this,
                arrayOf(UriUtils.uri2File(it).absolutePath),
                arrayOf(mime)
            ) { _, uri ->
                Log.d("MediaScanner", "Scanned $path: $uri")
            }
        }
    }
}

private fun Context.saveVideoToGallery(path: String) {
    val mime = getMimeType(FileUtils.getFileExtension(path))
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, FileUtils.getFileNameNoExtension(path))
        put(MediaStore.Video.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
    }
    contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let {
        contentResolver.openOutputStream(it)?.use { outputStream ->
            File(path).inputStream().use { inputStream ->
                inputStream.copyTo(outputStream, 1024 * 1024 * 2)
                MediaScannerConnection.scanFile(
                    this,
                    arrayOf(UriUtils.uri2File(it).absolutePath),
                    arrayOf(mime)
                ) { _, uri ->
                    Log.d("MediaScanner", "Scanned $path: $uri")
                }
            }
        }
    }
}
