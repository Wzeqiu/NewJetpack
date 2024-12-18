package com.common.kt

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.Utils
import com.common.common.R
import com.hjq.permissions.XXPermissions
import java.io.File


fun AppCompatActivity.requestPermission(
    vararg permission: String = arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO),
    action: () -> Unit = {}
) {
    XXPermissions.with(this).permission(permission)
        .request { p0, p1 -> if (p1) action() }
}

fun android.app.Fragment.requestPermission(vararg permission: String, action: () -> Unit = {}) {
    XXPermissions.with(this).permission(permission)
        .request { p0, p1 -> if (p1) action() }
}


inline fun <reified AT> AppCompatActivity.toActivity(vararg pairs: Pair<String, Any?>) {
    startActivity(toIntent<AT>(*pairs))
}

inline fun <reified AT> Fragment.toActivity(vararg pairs: Pair<String, Any?>) {
    startActivity(toIntent<AT>(*pairs))
}

inline fun <reified AT> AppCompatActivity.toIntent(vararg pairs: Pair<String, Any?>): Intent {
    return Intent(this, AT::class.java).putExtras(bundleOf(*pairs))
}

inline fun <reified AT> Fragment.toIntent(vararg pairs: Pair<String, Any?>): Intent {
    return Intent(requireActivity(), AT::class.java).putExtras(bundleOf(*pairs))
}


fun AppCompatActivity.saveToAlbum(path: String) {
    requestPermission(MANAGE_EXTERNAL_STORAGE) {
        saveVideoToGallery(path)

    }
}

private fun Context.saveImageToGallery(path: String) {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, FileUtils.getFileNameNoExtension(path))
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${Utils.getApp().getString(R.string.app_name)}/")
        }
    }
    contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let {
        contentResolver.openOutputStream(it)?.use { outputStream ->
            ImageUtils.getBitmap(path).compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            MediaScannerConnection.scanFile(this, arrayOf(UriUtils.uri2File(it).absolutePath), arrayOf("image/jpeg")) { _, uri ->
                Log.d("MediaScanner", "Scanned $path: $uri")
            }
        }
    }
}

private fun Context.saveVideoToGallery(path: String) {
    val values = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, FileUtils.getFileNameNoExtension(path))
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }
    }
    contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let {
        contentResolver.openOutputStream(it)?.use { outputStream ->
            File(path).inputStream().use { inputStream ->
                inputStream.copyTo(outputStream,1024 * 1024 * 2)
                MediaScannerConnection.scanFile(this, arrayOf(UriUtils.uri2File(it).absolutePath), arrayOf("video/mp4")) { _, uri ->
                    Log.d("MediaScanner", "Scanned $path: $uri")
                }
            }
        }
    }
}


