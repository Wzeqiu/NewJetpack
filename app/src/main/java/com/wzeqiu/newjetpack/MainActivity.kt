package com.wzeqiu.newjetpack

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.common.kt.singleClick
import com.common.kt.viewBinding
import com.common.kt.download.DownloadManager
import com.common.kt.download.DownloadManager.bindLifeDownloadFile
import com.wzeqiu.newjetpack.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import java.io.File
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    val viewBinding by viewBinding<ActivityMainBinding>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        viewBinding.textView.singleClick {
            for (i in 0..100) {
                this@MainActivity.bindLifeDownloadFile(
                    "https://upload-images.jianshu.io/upload_images/7274003-bb9beaa28834a74c.png?imageMogr2/auto-orient/strip|imageView2/2/w/685/format/webp",
                    File(cacheDir, "${System.currentTimeMillis()}_${Random.nextInt(10000000)}.jpg"), object : DownloadManager.DownloadListener {
                        override fun onDownloadProgress(progress: Int) {
                            Log.e("AAAA", "onDownloadProgress===$progress")
                        }

                        override fun onDownloadError(throwable: Throwable) {
                            Log.e("AAAA", "onDownloadError===$throwable")
                        }
                    }
                )
            }


        }

    }
}