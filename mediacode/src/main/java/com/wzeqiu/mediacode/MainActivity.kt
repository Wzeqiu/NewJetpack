package com.wzeqiu.mediacode

import android.R.attr.mimeType
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.common.activity.MediaInfo
import com.common.activity.MediaSelectionActivity
import com.common.kt.toIntent
import com.common.kt.viewBinding
import com.wzeqiu.mediacode.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityMainBinding>()
    private val videoDecoder by lazy { VideoDecoder() }

    private val videoSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val info = it.data?.getParcelableExtra<MediaInfo>(MediaSelectionActivity.KEY_RESULT)
            info?.let {
                videoDecoder.setDataSource(it.path)
            }
        }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.apply {
            surface.holder.addCallback(videoDecoder)



            addVideo.setOnClickListener {
                videoSelect.launch(toIntent<MediaSelectionActivity>())
            }

            start.setOnClickListener {
                videoDecoder.start()
            }

        }
    }
}