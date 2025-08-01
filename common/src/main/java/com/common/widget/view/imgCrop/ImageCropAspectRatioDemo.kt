package com.common.widget.view.imgCrop

import android.graphics.RectF

/**
 * 图片裁剪长宽比功能演示
 */
class ImageCropAspectRatioDemo {
    
    /**
     * 在Activity中设置长宽比的示例
     */
    fun setupAspectRatioExample(imageCropView: ImageCropView, cropHelper: ImageCropHelper) {
        
        // 1. 设置图片

        // 2. 设置裁剪框变化监听
        imageCropView.setOnCropChangeListener(object : ImageCropView.OnCropChangeListener {
            override fun onCropRectChanged(cropRect: RectF) {
                // 裁剪框变化时的回调
                val currentRatio = cropRect.width() / cropRect.height()
                println("当前裁剪框比例: ${String.format("%.2f", currentRatio)}")
            }
        })
        
        // 3. 使用预设的长宽比
        
        // 设置为正方形 1:1
        imageCropView.setAspectRatio(ImageCropView.ASPECT_RATIO_1_1)
        
        // 或者使用Helper类的预设
        cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_4_3)
        
        // 设置为16:9宽屏比例
        cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_16_9)
        
        // 设置为9:16竖屏比例  
        cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_9_16)
        
        // 设置自由比例（不限制）
        cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_FREE)
        
        // 4. 动态控制长宽比锁定
        
        // 锁定长宽比
        cropHelper.setAspectRatioLocked(true)
        
        // 解锁长宽比
        cropHelper.setAspectRatioLocked(false)
        
        // 检查当前状态
        val isLocked = cropHelper.isAspectRatioLocked()
        val currentRatio = cropHelper.getAspectRatio()
        
        println("长宽比锁定状态: $isLocked")
        println("当前长宽比: $currentRatio")
    }
    
    /**
     * 按钮点击事件处理示例
     */
    fun setupButtonClickHandlers(cropHelper: ImageCropHelper) {
        
        // 自由比例按钮
        // btnRatioFree.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_FREE)
        // }
        
        // 1:1 正方形按钮
        // btnRatio1_1.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_1_1)
        // }
        
        // 4:3 按钮
        // btnRatio4_3.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_4_3)
        // }
        
        // 3:4 按钮
        // btnRatio3_4.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_3_4)
        // }
        
        // 16:9 按钮
        // btnRatio16_9.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_16_9)
        // }
        
        // 9:16 按钮
        // btnRatio9_16.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_9_16)
        // }
        
        // 3:2 按钮
        // btnRatio3_2.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_3_2)
        // }
        
        // 5:4 按钮
        // btnRatio5_4.setOnClickListener {
        //     cropHelper.setAspectRatio(ImageCropHelper.AspectRatio.RATIO_5_4)
        // }
    }
    
    /**
     * 自定义长宽比示例
     */
    fun customAspectRatioExample(cropHelper: ImageCropHelper) {
        
        // 自定义比例，例如黄金比例 1.618:1
        val goldenRatio = 1.618f
        cropHelper.setAspectRatio(goldenRatio)
        
        // 自定义比例，例如A4纸张比例
        val a4Ratio = 1.414f  // √2
        cropHelper.setAspectRatio(a4Ratio)
        
        // 自定义比例，例如电影院宽屏 2.35:1
        val cinematicRatio = 2.35f
        cropHelper.setAspectRatio(cinematicRatio)
    }
    
    /**
     * 动态切换长宽比示例
     */
    fun dynamicAspectRatioExample(cropHelper: ImageCropHelper) {
        // 可以动态切换不同的长宽比
        val ratios = arrayOf(
            ImageCropHelper.AspectRatio.RATIO_1_1,
            ImageCropHelper.AspectRatio.RATIO_4_3,
            ImageCropHelper.AspectRatio.RATIO_16_9,
            ImageCropHelper.AspectRatio.RATIO_3_2
        )
        
        var currentIndex = 0
        
        // 每3秒切换一次比例（示例代码）
        // Timer().scheduleAtFixedRate(object : TimerTask() {
        //     override fun run() {
        //         Handler(Looper.getMainLooper()).post {
        //             cropHelper.setAspectRatio(ratios[currentIndex])
        //             currentIndex = (currentIndex + 1) % ratios.size
        //         }
        //     }
        // }, 0, 3000)
    }
    
    /**
     * 获取所有可用的长宽比预设
     */
    fun getAllAspectRatios(): Map<String, Float> {
        return mapOf(
            "自由比例" to ImageCropHelper.AspectRatio.RATIO_FREE,
            "正方形 1:1" to ImageCropHelper.AspectRatio.RATIO_1_1,
            "横向 4:3" to ImageCropHelper.AspectRatio.RATIO_4_3,
            "竖向 3:4" to ImageCropHelper.AspectRatio.RATIO_3_4,
            "宽屏 16:9" to ImageCropHelper.AspectRatio.RATIO_16_9,
            "竖屏 9:16" to ImageCropHelper.AspectRatio.RATIO_9_16,
            "相机 3:2" to ImageCropHelper.AspectRatio.RATIO_3_2,
            "竖向 2:3" to ImageCropHelper.AspectRatio.RATIO_2_3,
            "显示器 5:4" to ImageCropHelper.AspectRatio.RATIO_5_4,
            "竖向 4:5" to ImageCropHelper.AspectRatio.RATIO_4_5
        )
    }
}