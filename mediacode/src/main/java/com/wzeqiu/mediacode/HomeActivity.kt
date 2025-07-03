package com.wzeqiu.mediacode

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ActivityUtils.startActivity
import com.common.kt.activity.launch
import com.common.kt.saveToAlbum
import com.common.kt.viewBinding
import com.common.media.MediaConfig
import com.common.media.MediaConfig.Companion.MEDIA_TYPE_IMAGE
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.wzeqiu.mediacode.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityHomeBinding>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.apply {
            btVideo.setOnClickListener {
                startActivity(VideoActivity::class.java)
            }
            btImag.setOnClickListener {
                this@HomeActivity.launch(
                    MediaManageActivity.getIntent(
                        this@HomeActivity, MediaConfig(MEDIA_TYPE_IMAGE, originalMedia = false, enableMultiSelect = true, maxSelectCount = 3)
                    )
                ){
                    if (it.resultCode == RESULT_OK){
                        val data = it.data?.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA) as List<MediaInfo>
                        Log.e("AAAAAA","data==="+data.size)
                        saveToAlbum(data.map { it.path })
                    }

                }
            }
        }
    }
}