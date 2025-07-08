package com.wzeqiu.mediacode

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.Utils
import com.common.kt.activity.launch
import com.common.kt.saveToAlbum
import com.common.kt.viewBinding
import com.common.media.MediaConfig
import com.common.media.MediaConfig.Companion.MEDIA_TYPE_IMAGE
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.common.utils.WatermarkUtils
import com.wzeqiu.mediacode.databinding.ActivityHomeBinding
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class HomeActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityHomeBinding>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.apply {
            btVideo.setOnClickListener {
                startActivity(Intent(this@HomeActivity, VideoActivity::class.java))
            }
            
            // 添加跳转到音视频处理示例Activity的按钮点击事件
            btMediaProcessor.setOnClickListener {
                startActivity(Intent(this@HomeActivity, MediaProcessorSampleActivity::class.java))
            }
            
            btImag.setOnClickListener {
                this@HomeActivity.launch(
                    MediaManageActivity.getIntent(
                        this@HomeActivity,
                        MediaConfig(
                            MEDIA_TYPE_IMAGE,
                            originalMedia = false,
                            enableMultiSelect = true,
                            maxSelectCount = 3
                        )
                    )
                ) {
                    if (it.resultCode == RESULT_OK) {
                        val data =
                            it.data?.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA) as List<MediaInfo>
                        Log.e("AAAAAA", "data===" + data.size)

                        lifecycleScope.launch {
                            data.forEach {
                                Log.e("AAAAAA", "path===" + it.path)
                                val newFile1 = File(
                                    cacheDir,
                                    "${System.currentTimeMillis()}_${Random.nextInt(1000000)}.jpg"
                                )
                                WatermarkUtils.addWatermarkToImage(
                                    Utils.getApp(),
                                    File(it.path),
                                    "https://cdn.chengdujingqian.com/media/default/2507/03/1751546285_XME5TipQHS.png",
                                    newFile1,
                                )
                                Log.e("AAAAAA", "newFile1 path===" + newFile1.absolutePath)
                                saveToAlbum(mutableListOf(newFile1.absolutePath))
                            }
                        }


                    }

                }
            }
        }
    }
}