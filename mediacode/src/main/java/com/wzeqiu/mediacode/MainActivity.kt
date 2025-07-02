package com.wzeqiu.mediacode

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.common.kt.viewBinding
import com.common.media.MediaConfig
import com.common.media.MediaConfig.Companion.MEDIA_TYPE_IMAGE
import com.common.media.MediaConfig.Companion.MEDIA_TYPE_VIDEO
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.wzeqiu.mediacode.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityMainBinding>()
    private val videoDecoder by lazy { VideoDecoder() }

    // 单选视频
    private val videoSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val info = it.data?.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)
            info?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val newFile = File(cacheDir, "${System.currentTimeMillis()}.mp4")
                        Log.d("MainActivity", "开始裁剪视频: ${info.path} -> ${newFile.absolutePath}")
                        Log.d("MainActivity", "视频时长: ${info.duration}毫秒")
                        
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

    // 多选图片
    private val multiImageSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val infoList = it.data?.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA)
            infoList?.let { images ->
                val paths = images.joinToString("\n") { image -> image.path }
                viewBinding.tvResult.text = "已选择${images.size}张图片：\n$paths"
            }
        }

    // 多选视频
    private val multiVideoSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val infoList = it.data?.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA)
            infoList?.let { videos ->
                val summary = videos.joinToString("\n") { video -> 
                    "${video.name} (${video.getFormattedDuration()}, ${video.getFormattedSize()})"
                }
                viewBinding.tvResult.text = "已选择${videos.size}个视频：\n$summary"
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

            // 添加单个视频按钮
            addVideo.setOnClickListener {
                videoSelect.launch(
                    MediaManageActivity.getIntent(
                        this@MainActivity, MediaConfig(MEDIA_TYPE_VIDEO)
                    )
                )
            }
            
            // 添加多选图片按钮
            addImages.setOnClickListener {
                multiImageSelect.launch(
                    MediaManageActivity.getIntent(
                        this@MainActivity,
                        MediaConfig(
                            mediaType = MEDIA_TYPE_IMAGE,
                            enableMultiSelect = true,
                            maxSelectCount = 5
                        )
                    )
                )
            }
            
            // 添加多选视频按钮
            addVideos.setOnClickListener {
                multiVideoSelect.launch(
                    MediaManageActivity.getIntent(
                        this@MainActivity,
                        MediaConfig(
                            mediaType = MEDIA_TYPE_VIDEO,
                            enableMultiSelect = true,
                            maxSelectCount = 3
                        )
                    )
                )
            }

            start.setOnClickListener {
                videoDecoder.start()
            }
        }
    }
} 