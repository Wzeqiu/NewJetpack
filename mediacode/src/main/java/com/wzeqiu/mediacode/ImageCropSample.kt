package com.wzeqiu.mediacode

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ImageUtils
import com.common.common.R
import com.common.kt.activity.launch
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.common.utils.media.saveToAlbum
import com.common.widget.view.imgCrop.ImageCropHelper
import com.common.widget.view.imgCrop.ImageCropView
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import java.io.File

/**
 * 图片裁剪使用示例
 * 展示如何使用 ImageCropView 和 ImageCropHelper
 */
class ImageCropSample : AppCompatActivity() {

    private lateinit var imageCropView: ImageCropView
    private lateinit var cropHelper: ImageCropHelper
    

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sample_image_crop)
        
        initViews()
        setupCropHelper()
        setupClickListeners()
    }

    private fun initViews() {
        imageCropView = findViewById(R.id.imageCropView)
        
        // 设置裁剪框变化监听
        imageCropView.setOnCropChangeListener(object : ImageCropView.OnCropChangeListener {
            override fun onCropRectChanged(cropRect: RectF) {
                // 裁剪框变化时的回调
                // 可以在这里更新UI或执行其他操作
            }
        })
    }

    private fun setupCropHelper() {
        cropHelper = ImageCropHelper(imageCropView, lifecycleScope)
        
        cropHelper.setOnCropResultListener(object : ImageCropHelper.OnCropResultListener {
            override fun onCropSuccess(croppedBitmap: Bitmap) {
                // 裁剪成功
                Toast.makeText(this@ImageCropSample, "裁剪成功", Toast.LENGTH_SHORT).show()
                // 这里可以显示裁剪结果或保存到相册
                showCroppedImage(croppedBitmap)
            }

            override fun onCropFailure(error: String) {
                // 裁剪失败
                Toast.makeText(this@ImageCropSample, "裁剪失败: $error", Toast.LENGTH_SHORT).show()
            }

            override fun onCropProgress(isLoading: Boolean) {
                // 显示或隐藏加载状态
                // 可以在这里显示进度条
            }
        })
    }

    private fun setupClickListeners() {
        // 选择图片按钮
        findViewById<Button>(R.id.btnSelectImage).setOnClickListener {
            selectImage()
        }

        // 裁剪按钮
        findViewById<Button>(R.id.btnCrop).setOnClickListener {
            cropHelper.performCrop()
        }

        // 圆形裁剪
        findViewById<Button>(R.id.btnCircle).setOnClickListener {
            cropHelper.cropToCircle()
        }

        // 圆角裁剪
        findViewById<Button>(R.id.btnRounded).setOnClickListener {
            cropHelper.cropToRoundedRect(50f) // 50像素圆角
        }

        // 旋转图片
        findViewById<Button>(R.id.btnRotate).setOnClickListener {
            cropHelper.rotateImage(90f) // 顺时针旋转90度
        }

        // 翻转图片
        findViewById<Button>(R.id.btnFlip).setOnClickListener {
            cropHelper.flipImage(horizontal = true, vertical = false) // 水平翻转
        }

        // 1:1 比例
        findViewById<Button>(R.id.btnRatio1_1).setOnClickListener {
            imageCropView.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_1_1)
//            cropHelper.cropByAspectRatio(ImageCropHelper.AspectRatio.RATIO_1_1)
        }

        // 4:3 比例
        findViewById<Button>(R.id.btnRatio4_3).setOnClickListener {
            cropHelper.cropByAspectRatio(ImageCropHelper.AspectRatio.RATIO_4_3)
        }

        // 16:9 比例
        findViewById<Button>(R.id.btnRatio16_9).setOnClickListener {
            cropHelper.cropByAspectRatio(ImageCropHelper.AspectRatio.RATIO_16_9)
        }
    }

    /**
     * 选择图片
     */
    private fun selectImage() {
        launch( MediaManageActivity.getIntent(
            this@ImageCropSample, MediaConfig(MediaConfig.Companion.MEDIA_TYPE_IMAGE)
        )){
            if (it.resultCode == RESULT_OK) {
                it.data?.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)?.let { media ->
                    loadImageFromUri(media.path)
                }

            }
        }
    }

    /**
     * 从URI加载图片
     */
    private fun loadImageFromUri(uri: String) {
        cropHelper.setImageUri(uri)
    }

    /**
     * 显示裁剪结果
     */
    private fun showCroppedImage(bitmap: Bitmap) {
        // 或者保存到文件
        saveBitmapToFile(bitmap)
    }

    /**
     * 保存位图到文件
     */
    private fun saveBitmapToFile(bitmap: Bitmap) {
        XXPermissions.with(this@ImageCropSample)
            .permission(PermissionLists.getWriteExternalStoragePermission())
            .request { permissions, allGranted ->
                if (allGranted) {
                    val fileName = "cropped_${System.currentTimeMillis()}.jpg"
                    val outputFile = File(getExternalFilesDir("crops"), fileName)
                    ImageUtils.save(bitmap,outputFile, Bitmap.CompressFormat.JPEG)
//        cropHelper.cropAndSave(outputFile, 90)
                    saveToAlbum(outputFile.absolutePath)
                }

            }

    }
}

/**
 * 简化的使用方式示例
 */
object ImageCropUsageExample {
    
    /**
     * 在Fragment中使用
     */
    fun useInFragment(fragment: Fragment, imageCropView: ImageCropView) {
        val helper = ImageCropHelper(
            imageCropView,
            fragment.viewLifecycleOwner.lifecycleScope
        )
        
        helper.setOnCropResultListener(object : ImageCropHelper.OnCropResultListener {
            override fun onCropSuccess(croppedBitmap: Bitmap) {
                // 处理裁剪成功
            }
            
            override fun onCropFailure(error: String) {
                // 处理裁剪失败
            }
            
            override fun onCropProgress(isLoading: Boolean) {
                // 处理加载状态
            }
        })
        
        // 设置图片资源
        imageCropView.setImageResource(com.wzeqiu.mediacode.R.mipmap.ic_launcher)
        
        // 执行裁剪
        helper.performCrop()
    }
    
    /**
     * 直接使用ImageCropView
     */
    fun useDirectly(imageCropView: ImageCropView) {
        // 设置图片
        imageCropView.setImageResource(com.wzeqiu.mediacode.R.mipmap.ic_launcher)

        // 设置裁剪框变化监听
        imageCropView.setOnCropChangeListener(object : ImageCropView.OnCropChangeListener {
            override fun onCropRectChanged(cropRect: RectF) {
                // 裁剪框变化
            }
        })

        // 获取裁剪结果
        val croppedBitmap = imageCropView.getCroppedBitmap()

        // 重置裁剪区域
        imageCropView.resetCropRect()
    }
}