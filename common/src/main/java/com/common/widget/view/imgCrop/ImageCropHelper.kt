package com.common.widget.view.imgCrop

import android.graphics.Bitmap
import androidx.lifecycle.LifecycleCoroutineScope
import com.blankj.utilcode.util.ImageUtils
import com.common.widget.view.imgCrop.ImageCropUtils
import kotlinx.coroutines.launch
import java.io.File

/**
 * 图片裁剪助手类
 * 封装了 ImageCropView 的常用操作
 */
class ImageCropHelper(
    private val cropView: ImageCropView,
    private val lifecycleScope: LifecycleCoroutineScope
) {

    /**
     * 裁剪回调接口
     */
    interface OnCropResultListener {
        fun onCropSuccess(croppedBitmap: Bitmap)
        fun onCropFailure(error: String)
        fun onCropProgress(isLoading: Boolean)
    }

    private var cropResultListener: OnCropResultListener? = null

    fun setOnCropResultListener(listener: OnCropResultListener?) {
        this.cropResultListener = listener
    }

    /**
     * 设置图片并开始裁剪
     */
    fun setImageUri(uri: String) {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                // 使用 ImageCropUtils 加载图片
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap != null) {
                    cropView.setImageBitmap(bitmap)
                } else {
                    cropResultListener?.onCropFailure("加载图片失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("加载图片异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 执行裁剪操作
     */
    fun performCrop() {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                val croppedBitmap = cropView.getCroppedBitmap()
                if (croppedBitmap != null) {
                    cropResultListener?.onCropSuccess(croppedBitmap)
                } else {
                    cropResultListener?.onCropFailure("裁剪失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("裁剪异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 裁剪并保存到文件
     */
    fun cropAndSave(outputFile: File, quality: Int = 90) {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                val croppedBitmap = cropView.getCroppedBitmap()
                if (croppedBitmap != null) {
                    val success = ImageCropUtils.run {
                        saveBitmapToFile(croppedBitmap, outputFile, quality)
                    }
                    
                    if (success) {
                        cropResultListener?.onCropSuccess(croppedBitmap)
                    } else {
                        cropResultListener?.onCropFailure("保存文件失败")
                    }
                } else {
                    cropResultListener?.onCropFailure("裁剪失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("操作异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 裁剪为圆形
     */
    fun cropToCircle() {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                val croppedBitmap = cropView.getCroppedBitmap()
                if (croppedBitmap != null) {
                    val circleBitmap = ImageCropUtils.cropToCircle(croppedBitmap)
                    if (circleBitmap != null) {
                        cropResultListener?.onCropSuccess(circleBitmap)
                    } else {
                        cropResultListener?.onCropFailure("圆形裁剪失败")
                    }
                } else {
                    cropResultListener?.onCropFailure("裁剪失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("操作异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 裁剪为圆角矩形
     */
    fun cropToRoundedRect(cornerRadius: Float) {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                val croppedBitmap = cropView.getCroppedBitmap()
                if (croppedBitmap != null) {
                    val roundedBitmap = ImageCropUtils.cropToRoundedRect(croppedBitmap, cornerRadius)
                    if (roundedBitmap != null) {
                        cropResultListener?.onCropSuccess(roundedBitmap)
                    } else {
                        cropResultListener?.onCropFailure("圆角裁剪失败")
                    }
                } else {
                    cropResultListener?.onCropFailure("裁剪失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("操作异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 按比例裁剪
     */
    fun cropByAspectRatio(aspectRatio: Float) {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                val croppedBitmap = cropView.getCroppedBitmap()
                if (croppedBitmap != null) {
                    val ratioBitmap = ImageCropUtils.cropByAspectRatio(croppedBitmap, aspectRatio)
                    if (ratioBitmap != null) {
                        cropResultListener?.onCropSuccess(ratioBitmap)
                    } else {
                        cropResultListener?.onCropFailure("比例裁剪失败")
                    }
                } else {
                    cropResultListener?.onCropFailure("裁剪失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("操作异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 旋转图片
     */
    fun rotateImage(degrees: Float) {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                // 这里我们需要获取当前的bitmap并旋转它
                // 注意：这会重新设置图片，裁剪框会重置
                val currentBitmap = getCurrentBitmap()
                if (currentBitmap != null) {
                    val rotatedBitmap = ImageCropUtils.rotateBitmap(currentBitmap, degrees)
                    if (rotatedBitmap != null) {
                        cropView.setImageBitmap(rotatedBitmap)
                    } else {
                        cropResultListener?.onCropFailure("旋转失败")
                    }
                } else {
                    cropResultListener?.onCropFailure("获取当前图片失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("操作异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 翻转图片
     */
    fun flipImage(horizontal: Boolean = true, vertical: Boolean = false) {
        lifecycleScope.launch {
            cropResultListener?.onCropProgress(true)
            
            try {
                val currentBitmap = getCurrentBitmap()
                if (currentBitmap != null) {
                    val flippedBitmap = ImageCropUtils.flipBitmap(currentBitmap, horizontal, vertical)
                    if (flippedBitmap != null) {
                        cropView.setImageBitmap(flippedBitmap)
                    } else {
                        cropResultListener?.onCropFailure("翻转失败")
                    }
                } else {
                    cropResultListener?.onCropFailure("获取当前图片失败")
                }
            } catch (e: Exception) {
                cropResultListener?.onCropFailure("操作异常: ${e.message}")
            } finally {
                cropResultListener?.onCropProgress(false)
            }
        }
    }

    /**
     * 重置裁剪区域
     */
    fun resetCropRect() {
        cropView.resetCropRect()
    }

    /**
     * 获取当前显示的bitmap（私有方法，通过反射或其他方式获取）
     * 注意：这里需要根据实际的ImageCropView实现来获取当前bitmap
     */
    private fun getCurrentBitmap(): Bitmap? {
        // 这里应该从cropView获取当前的bitmap
        // 由于我们的ImageCropView没有公开获取原始bitmap的方法，
        // 可以考虑在ImageCropView中添加一个getCurrentBitmap()方法
        return null
    }

    /**
     * 从URI加载bitmap的简化版本
     */
    private suspend fun loadBitmapFromUri(uri: String): Bitmap? {
        return try {
            ImageUtils.getBitmap(uri,1080,1920)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置裁剪框长宽比
     */
    fun setAspectRatio(ratio: Float) {
        cropView.setAspectRatio(ratio)
    }
    
    /**
     * 获取当前长宽比
     */
    fun getAspectRatio(): Float = cropView.getAspectRatio()
    
    /**
     * 设置是否锁定长宽比
     */
    fun setAspectRatioLocked(locked: Boolean) {
        cropView.setAspectRatioLocked(locked)
    }
    
    /**
     * 是否锁定长宽比
     */
    fun isAspectRatioLocked(): Boolean = cropView.isAspectRatioLocked()

    /**
     * 常用的长宽比预设
     */
    object AspectRatio {
        const val RATIO_FREE = ImageCropView.ASPECT_RATIO_FREE   // 自由比例
        const val RATIO_1_1 = ImageCropView.ASPECT_RATIO_1_1     // 正方形 1:1
        const val RATIO_4_3 = ImageCropView.ASPECT_RATIO_4_3     // 4:3
        const val RATIO_3_4 = ImageCropView.ASPECT_RATIO_3_4     // 3:4
        const val RATIO_16_9 = ImageCropView.ASPECT_RATIO_16_9   // 16:9
        const val RATIO_9_16 = ImageCropView.ASPECT_RATIO_9_16   // 9:16
        const val RATIO_3_2 = ImageCropView.ASPECT_RATIO_3_2     // 3:2
        const val RATIO_2_3 = ImageCropView.ASPECT_RATIO_2_3     // 2:3
        const val RATIO_5_4 = ImageCropView.ASPECT_RATIO_5_4     // 5:4
        const val RATIO_4_5 = ImageCropView.ASPECT_RATIO_4_5     // 4:5
    }
}