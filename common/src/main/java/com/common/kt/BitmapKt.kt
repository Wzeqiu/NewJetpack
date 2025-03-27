package com.common.kt

import android.graphics.Bitmap
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.common.utils.PathManager
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
            PathManager.CACHE_IMAGE + File.separator + "${System.currentTimeMillis()}${Random.nextInt(1000000)}.jpg"
        if (ImageUtils.save(bitmap, newCachePath, Bitmap.CompressFormat.JPEG, true)) {
            it.resume(newCachePath)
        } else {
            FileUtils.delete(newCachePath)
            it.resume(null)
        }
    }
}

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
            PathManager.CACHE_IMAGE + File.separator + "${System.currentTimeMillis()}${Random.nextInt(100000)}.jpg"
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


