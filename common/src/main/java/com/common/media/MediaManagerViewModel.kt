package com.common.media

import android.app.Application
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.common.utils.PathManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.random.Random


class MediaManagerViewModel(application: Application) : AndroidViewModel(application) {
    val mediaSources = MutableLiveData<List<MediaInfo>>()

    fun getMediaSource(mediaConfig: MediaConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            when (mediaConfig.mediaType) {
                MediaConfig.MEDIA_TYPE_IMAGE -> mediaSources.postValue(getImageSource(this))
                MediaConfig.MEDIA_TYPE_VIDEO -> mediaSources.postValue(getVideoSource(this))
                MediaConfig.MEDIA_TYPE_AUDIO -> {}
            }
        }
    }


    /**
     *  获取系统图片资源
     */
    private fun getImageSource(scope: CoroutineScope): List<MediaInfo> {
        val images = mutableListOf<MediaInfo>()
        val queryImage = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA)
        val selection = MediaStore.Images.Media.MIME_TYPE + " IN (?, ?)"
        val selectionArgs = arrayOf("image/jpeg", "image/png")
        getApplication<Application>().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            queryImage, selection, selectionArgs,
            MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
        )?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                val pathIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                do {
                    val name = it.getString(nameIndex)
                    val path = it.getString(pathIndex)
                    images.add(MediaInfo(name, path, mediaType = MediaConfig.MEDIA_TYPE_IMAGE))
                } while (it.moveToNext() && scope.isActive)
            }
        }
        return images
    }

    /**
     * 获取系统视频资源
     */
    private fun getVideoSource(scope: CoroutineScope): List<MediaInfo> {
        val videos = mutableListOf<MediaInfo>()
        val queryVideo = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val selection = MediaStore.Video.Media.MIME_TYPE + "=?"
        val selectionArgs = arrayOf("video/mp4")
        getApplication<Application>().contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            queryVideo, selection, selectionArgs,
            MediaStore.Video.VideoColumns.DATE_MODIFIED + " DESC"
        )?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
                val pathIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DATA)
                val durationIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val sizeIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.SIZE)
                do {
                    val name = it.getString(nameIndex)
                    val path = it.getString(pathIndex)
                    val duration = it.getLong(durationIndex)
                    val size = it.getLong(sizeIndex)
                    videos.add(MediaInfo(name, path, size, duration, MediaConfig.MEDIA_TYPE_VIDEO))
                } while (it.moveToNext() && scope.isActive)
            }
        }
        return videos
    }


    private val maxImageWidth = 1080
    private val maxImageHeight = 1080


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

}