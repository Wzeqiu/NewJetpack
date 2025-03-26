package com.common.ui.media

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.common.common.databinding.ActivityMediaManageBinding
import com.common.kt.getCompressImagePath
import com.common.kt.requestPermission
import com.common.kt.toIntent
import com.common.kt.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaManageActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityMediaManageBinding>()
    private val mediaViewModel by viewModels<MediaManagerViewModel>()
    private val mediaConfig by lazy { intent.getParcelableExtra(BUNDLE_DATA) ?: MediaConfig() }
    private val mediaAdapter by lazy { MediaManageAdapter() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.apply {
            rvMedia.adapter = mediaAdapter
        }
        mediaAdapter.setOnItemClickListener { adapter, view, position ->
            val mediaInfo = adapter.getItem(position) ?: return@setOnItemClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                getCompressImagePath(mediaInfo.path)?.let {
                    mediaInfo.path=it
                    setResult(Activity.RESULT_OK, intent.putExtra(RESULT_DATA, mediaInfo))
                    finish()
                } ?: run {
                    // TODO: 重新选择
                }
            }

        }
        mediaViewModel.mediaSources.observe(this) {
            mediaAdapter.submitList(it)
        }
        requestPermission {
            mediaViewModel.getMediaSource(mediaConfig)
        }
    }

    companion object {
        const val BUNDLE_DATA = "BUNDLE_DATA"
        const val RESULT_DATA = "RESULT_DATA"

        fun getIntent(
            activity: AppCompatActivity,
            mediaConfig: MediaConfig
        ): Intent {
            return activity.toIntent<MediaManageActivity>(BUNDLE_DATA to mediaConfig)
        }
    }
}