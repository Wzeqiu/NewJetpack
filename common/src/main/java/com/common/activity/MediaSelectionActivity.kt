package com.common.activity

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.chad.library.adapter4.BaseQuickAdapter
import com.chad.library.adapter4.viewholder.QuickViewHolder
import com.common.common.R
import com.common.common.databinding.ActivityMediaSelectionBinding
import com.common.kt.requestPermission
import com.common.kt.viewBinding
import com.hjq.permissions.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlin.coroutines.resume

@Parcelize
class MediaInfo(
    val name: String, val path: String, val size: Long = 0, val duration: Long = 0
) : Parcelable {
    override fun toString(): String {
        return "MediaInfo(name='$name', path='$path', size=$size, duration=$duration)"
    }
}
suspend fun LifecycleOwner.getAllVideo(context: Context): MutableList<MediaInfo> {
    return suspendCancellableCoroutine {
        lifecycleScope.launch(Dispatchers.IO) {
            val videos = getMediaVideo(context)
            withContext(Dispatchers.Main) { it.resume(videos) }
        }
    }
}

fun getMediaVideo(context: Context): MutableList<MediaInfo> {
    val videos = mutableListOf<MediaInfo>()
    val queryVideo = arrayOf(
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE
    )
    context.applicationContext.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        queryVideo,
        null,
        null,
        MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
    )?.apply {
        if (moveToFirst()) {
            Log.e("getMediaVideo", "medias=====${count}")
            do {
                val nameIndex = getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
                val pathIndex = getColumnIndex(MediaStore.Video.VideoColumns.DATA)
                val durationIndex = getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val sizeIndex = getColumnIndex(MediaStore.Video.VideoColumns.SIZE)

                val name = getString(nameIndex)
                val path = getString(pathIndex)
                val duration = getLong(durationIndex)
                val size = getLong(sizeIndex)
                videos.add(MediaInfo(name, path, size, duration))

            } while (moveToNext())

        }
        close()
        Log.e("getMediaVideo", "medias=====${videos.size}")
    }
    return videos
}



class MediaSelectionActivity : AppCompatActivity() {
    val TAG = "MediaSelectionActivity"
    private val viewBinding by viewBinding<ActivityMediaSelectionBinding>()

    private val adapter by lazy {
        object : BaseQuickAdapter<MediaInfo, QuickViewHolder>() {
            override fun onBindViewHolder(
                holder: QuickViewHolder,
                position: Int,
                item: MediaInfo?
            ) {
                item ?: return
                val imageView = holder.getView<ImageView>(R.id.iv_img)
                Glide.with(this@MediaSelectionActivity).load(item.path).into(imageView)

            }

            override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int) =
                QuickViewHolder(R.layout.item_media, parent)

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.rvMedia.adapter = adapter


        adapter.setOnItemClickListener { adapter, view, position ->
            val item = adapter.getItem(position)
            setResult(RESULT_OK, intent.putExtra(KEY_RESULT, item))
            finish()
        }

        requestPermission(Permission.READ_MEDIA_VIDEO) {
            mediaData()
        }
    }


    private fun mediaData() {
        lifecycleScope.launch {
            val medias = getAllVideo(this@MediaSelectionActivity)
            Log.e(TAG, "medias=====${medias.size}")
            adapter.submitList(medias)
        }
    }


    companion object {
        const val KEY_RESULT = "key_result"
    }

}