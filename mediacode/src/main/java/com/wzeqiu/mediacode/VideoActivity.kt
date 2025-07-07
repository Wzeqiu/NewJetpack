package com.wzeqiu.mediacode

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.blankj.utilcode.util.Utils
import com.common.kt.saveToAlbum
import com.common.kt.viewBinding
import com.common.media.MediaConfig
import com.common.media.MediaConfig.Companion.MEDIA_TYPE_VIDEO
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.common.utils.WatermarkUtils
import com.wzeqiu.mediacode.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


@UnstableApi
class VideoActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityMainBinding>()
    private val videoDecoder by lazy { VideoDecoder() }

    private val videoSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val info = it.data?.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)
            info?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val newFile = File(cacheDir, "${System.currentTimeMillis()}.mp4")
                        val newFile1 = File(cacheDir, "${System.currentTimeMillis()}.mp4")
                        Log.d(
                            "MainActivity",
                            "开始裁剪视频: ${info.path} -> ${newFile.absolutePath}"
                        )
                        Log.d("MainActivity", "视频时长: ${info.duration}毫秒")

                        WatermarkUtils.addWatermarkToVideo(
                            this@VideoActivity,
                            File(info.path),
                            "https://cdn.chengdujingqian.com/media/default/2507/03/1751546285_XME5TipQHS.png",
                            newFile1,
                            WatermarkUtils.WatermarkPosition.RIGHT_BOTTOM
                            ,0.2f,0.1f,0.5f
                        )
                        saveToAlbum(mutableListOf(newFile1.absolutePath))


                        // 使用MediaClipper裁剪视频
                        val clipper = MediaClipper()
                        // 裁剪视频的前5秒，如果视频不足5秒则裁剪全部
                        val endTimeMs = minOf(5000, info.duration.toLong())
                        val startTimeUs = 0L // 从开始位置裁剪
                        val endTimeUs = endTimeMs * 1000 // 转换为微秒

                        clipper.clip(info.path, newFile.absolutePath, startTimeUs, endTimeUs)

                        runOnUiThread {
                            viewBinding.tvResult.text = "裁剪完成: ${newFile.absolutePath}"
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "裁剪视频失败", e)
                        runOnUiThread {
                            viewBinding.tvResult.text = "裁剪失败: ${e.message}"
                        }
                    }
                }

            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding.apply {
            surface.holder.addCallback(videoDecoder)

            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    videoDecoder.seekTo(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })



            addVideo.setOnClickListener {
                videoSelect.launch(
                    MediaManageActivity.getIntent(
                        this@VideoActivity, MediaConfig(MEDIA_TYPE_VIDEO)
                    )
                )
            }

            start.setOnClickListener {
                videoDecoder.start()
            }

        }
    }
}