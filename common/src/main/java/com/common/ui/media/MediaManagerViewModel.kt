package com.common.ui.media

import android.app.Application
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


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
        val selection = MediaStore.Images.Media.MIME_TYPE + "=?"
        val selectionArgs = arrayOf("image/jpeg", "image/png")
        getApplication<Application>().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            queryImage, selection, selectionArgs,
            MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
        )?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                val pathIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                while (it.moveToNext() && scope.isActive) {
                    val name = it.getString(nameIndex)
                    val path = it.getString(pathIndex)
                    images.add(MediaInfo(name, path, mediaType = MediaConfig.MEDIA_TYPE_IMAGE))
                }
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
                while (it.moveToNext() && scope.isActive) {
                    val name = it.getString(nameIndex)
                    val path = it.getString(pathIndex)
                    val duration = it.getLong(durationIndex)
                    val size = it.getLong(sizeIndex)
                    videos.add(MediaInfo(name, path, size, duration, MediaConfig.MEDIA_TYPE_VIDEO))
                }
            }
        }
        return videos
    }
}