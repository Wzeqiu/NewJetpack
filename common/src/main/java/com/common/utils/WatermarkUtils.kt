package com.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.util.Log
import androidx.media3.effect.BitmapOverlay
import com.blankj.utilcode.util.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 水印工具类，支持为图片和视频添加水印
 */
object WatermarkUtils {

    private const val TAG = "WatermarkUtils"

    /**
     * 水印位置枚举
     */
    enum class WatermarkPosition {
        LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, CENTER
    }

    /**
     * 给图片添加水印
     *
     * @param context 上下文
     * @param sourceBitmap 源图片
     * @param watermarkBitmap 水印图片
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于原图的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于原图的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 添加水印后的图片
     */
    fun addWatermarkToBitmap(
        sourceBitmap: Bitmap,
        watermarkBitmap: Bitmap,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.2f,
        marginFactor: Float = 0.05f,
        alpha: Int = 255
    ): Bitmap {
        val result = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // 计算水印大小
        val watermarkWidth = (sourceBitmap.width * sizeFactor).toInt()
        val watermarkHeight = (watermarkBitmap.height * watermarkWidth / watermarkBitmap.width)

        // 计算边距
        val margin = (sourceBitmap.width * marginFactor).toInt()

        // 调整水印大小
        val scaledWatermark = Bitmap.createScaledBitmap(
            watermarkBitmap,
            watermarkWidth,
            watermarkHeight.toInt(),
            true
        )

        // 设置水印位置
        val x: Int
        val y: Int
        when (position) {
            WatermarkPosition.LEFT_TOP -> {
                x = margin
                y = margin
            }
            WatermarkPosition.RIGHT_TOP -> {
                x = sourceBitmap.width - watermarkWidth - margin
                y = margin
            }
            WatermarkPosition.LEFT_BOTTOM -> {
                x = margin
                y = sourceBitmap.height - watermarkHeight.toInt() - margin
            }
            WatermarkPosition.RIGHT_BOTTOM -> {
                x = sourceBitmap.width - watermarkWidth - margin
                y = sourceBitmap.height - watermarkHeight.toInt() - margin
            }
            WatermarkPosition.CENTER -> {
                x = (sourceBitmap.width - watermarkWidth) / 2
                y = (sourceBitmap.height - watermarkHeight.toInt()) / 2
            }
        }

        // 绘制水印
        val paint = Paint().apply {
            this.alpha = alpha
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            // 使用SRC_OVER模式，让水印能够保留透明度
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        }
        canvas.drawBitmap(scaledWatermark, x.toFloat(), y.toFloat(), paint)

        // 释放资源
        scaledWatermark.recycle()
        
        return result
    }

    /**
     * 从网络加载水印图片并添加到源图片上
     *
     * @param context 上下文
     * @param sourceBitmap 源图片
     * @param watermarkUrl 水印图片URL
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于原图的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于原图的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 添加水印后的图片
     */
    suspend fun addWatermarkToBitmap(
        context: Context,
        sourceBitmap: Bitmap,
        watermarkUrl: String,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.2f,
        marginFactor: Float = 0.05f,
        alpha: Int = 255
    ): Bitmap = withContext(Dispatchers.IO) {
        val watermarkBitmap = loadBitmapFromUrl(context, watermarkUrl)
        val result = addWatermarkToBitmap(
            sourceBitmap,
            watermarkBitmap,
            position,
            sizeFactor,
            marginFactor,
            alpha
        )
        watermarkBitmap.recycle()
        return@withContext result
    }

    /**
     * 给图片文件添加水印
     *
     * @param context 上下文
     * @param sourceFile 源图片文件
     * @param watermarkFile 水印图片文件
     * @param destFile 目标文件
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于原图的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于原图的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 成功返回true，失败返回false
     */
    suspend fun addWatermarkToImage(
        context: Context,
        sourceFile: File,
        watermarkFile: File,
        destFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.2f,
        marginFactor: Float = 0.05f,
        alpha: Int = 255
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            val watermarkBitmap = BitmapFactory.decodeFile(watermarkFile.absolutePath)

            val resultBitmap = addWatermarkToBitmap(
                sourceBitmap,
                watermarkBitmap,
                position,
                sizeFactor,
                marginFactor,
                alpha
            )

            // 保存到文件
            FileOutputStream(destFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 释放资源
            sourceBitmap.recycle()
            watermarkBitmap.recycle()
            resultBitmap.recycle()

            true
        } catch (e: Exception) {
            Log.e(TAG, "添加水印失败", e)
            false
        }
    }

    /**
     * 给图片文件添加网络水印
     *
     * @param context 上下文
     * @param sourceFile 源图片文件
     * @param watermarkUrl 水印图片URL
     * @param destFile 目标文件
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于原图的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于原图的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 成功返回true，失败返回false
     */
    suspend fun addWatermarkToImage(
        context: Context,
        sourceFile: File,
        watermarkUrl: String,
        destFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.2f,
        marginFactor: Float = 0.05f,
        alpha: Int = 255
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath)
            val watermarkBitmap = loadBitmapFromUrl(context, watermarkUrl)

            val resultBitmap = addWatermarkToBitmap(
                sourceBitmap,
                watermarkBitmap,
                position,
                sizeFactor,
                marginFactor,
                alpha
            )

            // 保存到文件
            FileOutputStream(destFile).use { out ->
                resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 释放资源
            sourceBitmap.recycle()
            watermarkBitmap.recycle()
            resultBitmap.recycle()

            true
        } catch (e: Exception) {
            Log.e(TAG, "添加水印失败", e)
            false
        }
    }

    /**
     * 从URL加载Bitmap
     */
    private suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap = suspendCancellableCoroutine { continuation ->
        try {
            Glide.with(context)
                .asBitmap()
                .load(url)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        continuation.resume(resource)
                    }

                    override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                        continuation.resumeWithException(Exception("Failed to load watermark image from URL: $url"))
                    }
                })
            
            continuation.invokeOnCancellation {
                // 取消Glide的加载
                Glide.with(context).clear(null)
            }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

         /**
     * 给视频添加水印
     * 采用帧提取和重组方式，处理较慢但兼容性好
     *
     * @param context 上下文
     * @param inputVideoFile 输入视频文件
     * @param watermarkBitmap 水印位图
     * @param outputVideoFile 输出视频文件
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于视频的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于视频的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 成功返回true，失败返回false
     */
    suspend fun addWatermarkToVideo(
        context: Context,
        inputVideoFile: File,
        watermarkBitmap: Bitmap,
        outputVideoFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.1f,
        marginFactor: Float = 0.03f,
        alpha: Int = 255
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取视频尺寸
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputVideoFile.absolutePath)
            val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1920
            val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 1080
            
            // 获取视频时长(微秒)
            val durationUs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()?.times(1000) ?: 0L
            
            // 计算水印大小
            val watermarkWidth = (videoWidth * sizeFactor).toInt()
            val watermarkHeight = (watermarkBitmap.height * watermarkWidth / watermarkBitmap.width)
            
            // 调整水印大小
            val scaledWatermarkBitmap = Bitmap.createScaledBitmap(
                watermarkBitmap,
                watermarkWidth, 
                watermarkHeight.toInt(),
                true
            )

            // 计算水印位置
            val margin = (videoWidth * marginFactor).toInt()
            val x: Int
            val y: Int
            
            when (position) {
                WatermarkPosition.LEFT_TOP -> {
                    x = margin
                    y = margin
                }
                WatermarkPosition.RIGHT_TOP -> {
                    x = videoWidth - watermarkWidth - margin
                    y = margin
                }
                WatermarkPosition.LEFT_BOTTOM -> {
                    x = margin
                    y = videoHeight - watermarkHeight.toInt() - margin
                }
                WatermarkPosition.RIGHT_BOTTOM -> {
                    x = videoWidth - watermarkWidth - margin
                    y = videoHeight - watermarkHeight.toInt() - margin
                }
                WatermarkPosition.CENTER -> {
                    x = (videoWidth - watermarkWidth) / 2
                    y = (videoHeight - watermarkHeight.toInt()) / 2
                }
            }

            // 创建输出目录
            FileUtils.createOrExistsDir(outputVideoFile.parentFile)
            
            // 创建临时目录存放帧图片
            val tempDir = File(context.cacheDir, "video_frames_${System.currentTimeMillis()}")
            FileUtils.createOrExistsDir(tempDir)
            
            // 简易实现：提取关键帧并添加水印
            // 实际项目中应该使用更高效的视频处理库如FFmpeg
            val frameInterval = 1000000L // 每秒一帧
            var frameCount = 0
            
            // 提取关键帧并添加水印
            for (timeUs in 0L..durationUs step frameInterval) {
                val frameBitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    ?: continue
                
                // 添加水印
                val watermarkedFrame = addWatermarkToBitmap(
                    frameBitmap, 
                    scaledWatermarkBitmap,
                    position,
                    sizeFactor,
                    marginFactor,
                    alpha
                )
                
                // 保存帧
                val frameFile = File(tempDir, "frame_${frameCount++}.jpg")
                FileOutputStream(frameFile).use { out ->
                    watermarkedFrame.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                
                // 释放资源
                frameBitmap.recycle()
                watermarkedFrame.recycle()
            }
            
            retriever.release()
            scaledWatermarkBitmap.recycle()
            
            // 注意：这里只是示例，实际上需要用专业的视频处理库将帧重组为视频
            // 比如使用FFmpeg或MediaCodec API
            Log.d(TAG, "提取并水印处理了 $frameCount 帧")
            
            // 清理临时文件
            FileUtils.deleteAllInDir(tempDir)
            
            // 由于实现视频重组比较复杂，这里返回false
            // 实际项目中应使用FFmpeg或其他视频处理库
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "添加视频水印失败", e)
            return@withContext false
        }
    }

    /**
     * 给视频添加网络水印
     *
     * @param context 上下文
     * @param inputVideoFile 输入视频文件
     * @param watermarkUrl 水印图片URL
     * @param outputVideoFile 输出视频文件
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于视频的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于视频的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 成功返回true，失败返回false
     */
    suspend fun addWatermarkToVideo(
        context: Context,
        inputVideoFile: File,
        watermarkUrl: String,
        outputVideoFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.1f,
        marginFactor: Float = 0.03f,
        alpha: Int = 255
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val watermarkBitmap = loadBitmapFromUrl(context, watermarkUrl)
            val result = addWatermarkToVideo(
                context,
                inputVideoFile,
                watermarkBitmap,
                outputVideoFile,
                position,
                sizeFactor,
                marginFactor,
                alpha
            )
            watermarkBitmap.recycle()
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "添加视频水印失败", e)
            return@withContext false
        }
    }

    /**
     * 给视频添加本地水印
     *
     * @param context 上下文
     * @param inputVideoFile 输入视频文件
     * @param watermarkFile 水印图片文件
     * @param outputVideoFile 输出视频文件
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于视频的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于视频的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @return 成功返回true，失败返回false
     */
    suspend fun addWatermarkToVideoFromFile(
        context: Context,
        inputVideoFile: File,
        watermarkFile: File,
        outputVideoFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.1f,
        marginFactor: Float = 0.03f,
        alpha: Int = 255
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val watermarkBitmap = BitmapFactory.decodeFile(watermarkFile.absolutePath)
            val result = addWatermarkToVideo(
                context,
                inputVideoFile,
                watermarkBitmap,
                outputVideoFile,
                position,
                sizeFactor,
                marginFactor,
                alpha
            )
            watermarkBitmap.recycle()
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "添加视频水印失败", e)
            return@withContext false
        }
    }
    
    /**
     * 重要提示：完整实现视频水印功能需要使用FFmpeg等专业视频处理库
     * 可以添加以下依赖:
     * 1. 添加FFmpeg依赖：implementation 'com.arthenica:mobile-ffmpeg-full:4.4.LTS'
     * 2. 或者使用Media3 Transformer (需要更新配置)
     * 
     * 然后替换视频处理部分的实现
     */
}