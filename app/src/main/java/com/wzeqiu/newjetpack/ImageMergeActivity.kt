package com.wzeqiu.newjetpack

import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.createBitmap
import com.blankj.utilcode.util.ImageUtils
import com.blankj.utilcode.util.ScreenUtils
import com.common.kt.singleClick
import com.common.media.MediaConfig
import com.common.media.MediaConfig.Companion.MEDIA_TYPE_IMAGE
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.common.ui.BaseActivity
import com.gyf.immersionbar.ktx.immersionBar
import com.wzeqiu.newjetpack.databinding.ActivityImageMergeBinding
import kotlin.math.max

class ImageMergeActivity : BaseActivity<ActivityImageMergeBinding>() {
    // 多选图片
    private val multiImageSelect =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val infoList = it.data?.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA)

            infoList?.let { images ->

                // 实现两张图片水平拼接
                val bitmap1 = ImageUtils.getBitmap(images[0].path, 1920, 1920)
                val bitmap2 = ImageUtils.getBitmap(images[1].path, 1920, 1920)

                val newBitmapWidth= bitmap1.width+bitmap2.width
                val newBitmapHeight=max(bitmap1.height,bitmap2.height)
                val newBitmap = createBitmap(newBitmapWidth, newBitmapHeight)
                val canvas = Canvas(newBitmap)

                val bitmapRect1= Rect()
                val bitmapWidthScale=(newBitmapWidth/2f)/bitmap1.width
                val bitmapHeightScale=newBitmapHeight/bitmap1.height.toFloat()
                if (bitmapWidthScale>bitmapHeightScale){
                    val newWidth=bitmap1.width/bitmapWidthScale
                    val newHeight=bitmap1.height/bitmapWidthScale
                    val offSetY=(newHeight-newBitmapHeight).toInt()

                    bitmapRect1.set(0, -offSetY/2, newBitmapWidth/2, newBitmapHeight+offSetY/2)
                }else{
                    val newWidth=bitmap1.width*bitmapHeightScale
                    val newHeight=bitmapHeightScale/bitmap1.height
                    val offSetX=(newWidth-newBitmapWidth/2).toInt()

                    bitmapRect1.set(-offSetX, 0, newBitmapWidth/2, newBitmapHeight)
                }


                canvas.drawBitmap(bitmap1, Rect(0,0,bitmap1.width,bitmap1.height), bitmapRect1, null)

                val bitmapRect2= Rect()
                val bitmapWidthScale2=(newBitmapWidth/2f)/bitmap2.width
                val bitmapHeightScale2=newBitmapHeight/bitmap2.height.toFloat()
                if (bitmapWidthScale2>bitmapHeightScale2){
                    val newWidth=bitmap2.width/bitmapWidthScale2
                    val newHeight=bitmap2.height*bitmapWidthScale2
                    val offSetY=(newHeight-newBitmapHeight).toInt()

                    bitmapRect2.set(newBitmapWidth/2, -offSetY/2, newBitmapWidth, newBitmapHeight+offSetY/2)
                }else{
                    val newWidth=bitmap2.width*bitmapHeightScale2
                    val newHeight=bitmapHeightScale2/bitmap2.height
                    val offSetX=(newWidth-newBitmapWidth/2).toInt()

                    bitmapRect2.set(newBitmapWidth/2, 0, newBitmapWidth+offSetX, newBitmapHeight)
                }


                canvas.drawBitmap(bitmap2, Rect(0,0,bitmap2.width,bitmap2.height), bitmapRect2, null)
                binding.ivImageView.setImageBitmap(newBitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            immersionBar { titleBarMarginTop(main) }
            addImage.singleClick {
                multiImageSelect.launch(
                    MediaManageActivity.getIntent(
                        this@ImageMergeActivity,
                        MediaConfig(
                            mediaType = MEDIA_TYPE_IMAGE,
                            enableMultiSelect = true,
                            maxSelectCount = 2
                        )
                    )
                )
            }
        }


    }
}