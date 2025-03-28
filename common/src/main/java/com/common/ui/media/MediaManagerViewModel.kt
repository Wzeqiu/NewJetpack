package com.common.ui.media

import android.app.Application
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MediaManagerViewModel( val APP: Application) : AndroidViewModel(APP) {
    val mediaSources = MutableLiveData<List<MediaInfo>>()

    fun getMediaSource(mediaConfig: MediaConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            when (mediaConfig.mediaType) {
                MediaConfig.MEDIA_TYPE_IMAGE -> mediaSources.postValue(getImageSource())
                MediaConfig.MEDIA_TYPE_VIDEO -> mediaSources.postValue(getVideoSource())
                MediaConfig.MEDIA_TYPE_AUDIO -> {}
            }
        }
    }

    override fun <T : Application> getApplication(): T {
        return APP as T
    }

    private fun getImageSource(): List<MediaInfo> {
        val images = mutableListOf<MediaInfo>()
        val queryImage = arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA)
        val selection = MediaStore.Images.Media.MIME_TYPE + "=?"
        val selectionArgs = arrayOf("image/jpeg", "image/png")


        APP.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            queryImage, selection, selectionArgs,
            MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
        )?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                val pathIndex = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val path = it.getString(pathIndex)
                    images.add(MediaInfo(name, path, mediaType = MediaConfig.MEDIA_TYPE_IMAGE))
                }
            }
        }
        return images
    }

    private fun getVideoSource(): List<MediaInfo> {
        val videos = mutableListOf<MediaInfo>()
        val queryVideo = arrayOf(
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        val selection = MediaStore.Video.Media.MIME_TYPE + "=?"
        val selectionArgs = arrayOf("video/mp4")
        APP.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            queryVideo, selection, selectionArgs,
            MediaStore.Video.VideoColumns.DATE_MODIFIED + " DESC"
        )?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
                val pathIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DATA)
                val durationIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val sizeIndex = it.getColumnIndex(MediaStore.Video.VideoColumns.SIZE)
                it.moveToFirst()
                while (it.moveToNext()) {
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