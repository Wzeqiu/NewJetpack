package com.wzeqiu.newjetpack

import android.os.Bundle
import android.util.Log
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.UriUtils
import com.common.kt.requestPermission
import com.common.kt.saveToAlbum
import com.common.kt.singleClick
import com.common.kt.viewBinding
import com.wzeqiu.newjetpack.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    val viewBinding by viewBinding<ActivityMainBinding>()


    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        //在用户选择媒体项目或关闭照片选择器后调用回调。
        if (uri != null) {
            saveToAlbum(UriUtils.uri2File(uri).absolutePath)
        } else {
            Log.e("PhotoPicker", "No media selected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.textView.singleClick {
            requestPermission{
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
            }
        }

    }
}