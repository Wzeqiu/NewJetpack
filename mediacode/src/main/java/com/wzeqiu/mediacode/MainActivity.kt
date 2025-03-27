package com.wzeqiu.mediacode

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.common.kt.viewBinding
import com.common.ui.media.MediaConfig
import com.common.ui.media.MediaConfig.Companion.MEDIA_TYPE_VIDEO
import com.common.ui.media.MediaInfo
import com.common.ui.media.MediaManageActivity
import com.wzeqiu.mediacode.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityMainBinding>()
    private val videoDecoder by lazy { VideoDecoder() }

    private val videoSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val info = it.data?.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)
            info?.let {
                videoDecoder.setDataSource(it.path)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.apply {
            surface.holder.addCallback(videoDecoder)



            addVideo.setOnClickListener {
                videoSelect.launch(
                    MediaManageActivity.getIntent(
                        this@MainActivity, MediaConfig(MEDIA_TYPE_VIDEO)
                    )
                )
            }

            start.setOnClickListener {
                videoDecoder.start()
            }

        }
    }
}