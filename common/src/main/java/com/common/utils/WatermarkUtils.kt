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
import androidx.media3.common.MediaItem
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.blankj.utilcode.util.FileUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.media3.common.Effect
import androidx.media3.transformer.Effects
import androidx.core.graphics.scale
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.transformer.VideoEncoderSettings.RATE_UNSET
import kotlin.math.max

/**
 * 水印工具类，支持为图片和视频添加水印
 */
object WatermarkUtils {

    private const val TAG = "WatermarkUtils"

    // 水印位图缓存
    private val watermarkBitmapCache = mutableMapOf<String, Bitmap>()

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
        // 获取或加载水印位图
        val watermarkBitmap = getOrLoadWatermarkBitmap(context, watermarkUrl)
        val result = addWatermarkToBitmap(
            sourceBitmap,
            watermarkBitmap,
            position,
            sizeFactor,
            marginFactor,
            alpha
        )
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
            // 获取或加载水印位图
            val watermarkBitmap = getOrLoadWatermarkBitmap(context, watermarkUrl)

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

            // 释放资源 (只释放源图片和结果图片，不释放水印)
            sourceBitmap.recycle()
            resultBitmap.recycle()

            true
        } catch (e: Exception) {
            Log.e(TAG, "添加水印失败", e)
            false
        }
    }

    /**
     * 从URL加载Bitmap或从缓存获取
     */
    private suspend fun getOrLoadWatermarkBitmap(context: Context, url: String): Bitmap = withContext(Dispatchers.IO) {
        // 检查缓存中是否有该水印
        watermarkBitmapCache[url]?.let { return@withContext it }

        // 加载并缓存水印
        val bitmap = loadBitmapFromUrl(context, url)
        watermarkBitmapCache[url] = bitmap
        return@withContext bitmap
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
     * 给视频添加水印 (使用Media3 Transformer) - 兼容新版本API
     *
     * @param context 上下文
     * @param inputVideoFile 输入视频文件
     * @param watermarkBitmap 水印位图
     * @param outputVideoFile 输出视频文件
     * @param position 水印位置
     * @param sizeFactor 水印大小因子(相对于视频的比例，范围0.0-1.0)
     * @param marginFactor 水印边距因子(相对于视频的比例，范围0.0-1.0)
     * @param alpha 水印透明度(0-255)
     * @param recycleWatermark 是否回收水印图片资源，批量处理时应设为false
     * @return 成功返回true，失败返回false
     */
    @UnstableApi
    suspend fun addWatermarkToVideo(
        context: Context,
        inputVideoFile: File,
        watermarkBitmap: Bitmap,
        outputVideoFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.1f,
        marginFactor: Float = 0.03f,
        alpha: Float = 1f,
        recycleWatermark: Boolean = false // 默认不回收水印，适合批量处理
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 创建输出目录
            FileUtils.createOrExistsDir(outputVideoFile.parentFile)

            // 获取视频比特率、Profile和Level以保持质量
            val extractor = MediaExtractor()
            var videoBitrate = 8_000_000 // 默认8Mbps
            var videoProfile = -1
            var videoLevel = -1
            try {
                extractor.setDataSource(inputVideoFile.absolutePath)
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime?.startsWith("video/") == true) {
                        if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                            videoBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                        }
                        if (format.containsKey(MediaFormat.KEY_PROFILE)) {
                            videoProfile = format.getInteger(MediaFormat.KEY_PROFILE)
                        }
                        if (format.containsKey(MediaFormat.KEY_LEVEL)) {
                            videoLevel = format.getInteger(MediaFormat.KEY_LEVEL)
                        }
                        break
                    }
                }
                videoBitrate= max(videoBitrate,8_000_000)
            } catch (e: Exception) {
                Log.w(WatermarkUtils.TAG, "无法获取视频编码参数，将使用默认值", e)
            } finally {
                extractor.release()
            }
            Log.d(WatermarkUtils.TAG, "原视频参数 - Bitrate: $videoBitrate, Profile: $videoProfile, Level: $videoLevel")


            // 获取视频尺寸
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputVideoFile.absolutePath)
            val videoWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 1920
            val videoHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 1080
            retriever.release()

            // 计算水印大小
            val watermarkWidth = (videoWidth * sizeFactor).toInt()
            val watermarkHeight = (watermarkBitmap.height * watermarkWidth / watermarkBitmap.width)

            // 调整水印大小
            val scaledWatermarkBitmap = watermarkBitmap.scale(watermarkWidth, watermarkHeight,true)

            // 计算水印位置
            val bFrameX: Float
            val bFrameY: Float

            // 计算水印偏移
            val oFrameX: Float
            val oFrameY: Float

            when (position) {
                WatermarkUtils.WatermarkPosition.LEFT_TOP -> {
                    bFrameX =-1f+marginFactor
                    bFrameY = 1f-marginFactor
                    oFrameX=-1f
                    oFrameY=1f
                }
                WatermarkUtils.WatermarkPosition.RIGHT_TOP -> {
                    bFrameX =1f-marginFactor
                    bFrameY = 1f-marginFactor

                    oFrameX=1f
                    oFrameY=1f
                }
                WatermarkUtils.WatermarkPosition.LEFT_BOTTOM -> {
                    bFrameX =-1f+marginFactor
                    bFrameY = -1f+marginFactor

                    oFrameX=-1f
                    oFrameY=-1f
                }
                WatermarkUtils.WatermarkPosition.RIGHT_BOTTOM -> {
                    bFrameX = 1f-marginFactor
                    bFrameY = -1f+marginFactor
                    oFrameX=1f
                    oFrameY=-1f
                }
                WatermarkUtils.WatermarkPosition.CENTER -> {
                    bFrameX = 0f
                    bFrameY =0f
                    oFrameX=0f
                    oFrameY=0f
                }
            }

            // 将 [0, 1] 范围的比例坐标转换为 [-1, 1] 范围的NDC（归一化设备坐标）

            // 创建水印覆盖效果
            val overlaySettings = StaticOverlaySettings.Builder()
                .setAlphaScale(alpha )  // 设置透明度
                .setBackgroundFrameAnchor(bFrameX, bFrameY) // 使用根据position计算的NDC坐标
                .setOverlayFrameAnchor(oFrameX, oFrameY)
                .build()

            val bitmapOverlay: TextureOverlay = BitmapOverlay.createStaticBitmapOverlay(scaledWatermarkBitmap, overlaySettings)

            // 创建Media3转换任务
            val result = withContext(Dispatchers.Main) {
                suspendCancellableCoroutine<Boolean> { continuation ->
                    val listener = object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            Log.d(WatermarkUtils.TAG, "视频水印添加成功")
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exception: ExportException
                        ) {
                            Log.e(WatermarkUtils.TAG, "视频水印添加失败", exception)
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }
                    // 配置转换器，保留原始视频质量
                    val settingsBuilder = VideoEncoderSettings.Builder()    // 配置转换器，保留原始视频质量
                        .setBitrate(videoBitrate)
                        .setEncoderPerformanceParameters(30, RATE_UNSET)
                    if (videoProfile != -1 && videoLevel != -1) {
                        settingsBuilder.setEncodingProfileLevel(videoProfile,videoLevel)
                    }

                    val encoderFactory = DefaultEncoderFactory.Builder(context)
                        .setEnableFallback(true) // 关键：禁止回退到低质量的软件编码器
                        .setRequestedVideoEncoderSettings(settingsBuilder.build())
                        .build()

                    val transformer = Transformer.Builder(context)
                        .setEncoderFactory(encoderFactory)
                        .addListener(listener)
                        .build()

                    val mediaItem = MediaItem.fromUri(inputVideoFile.toURI().toString())
                    // 1. 将 TextureOverlay 包装在 OverlayEffect 中，这才是正确的 Effect 类型
                    val overlayEffect = OverlayEffect(listOf(bitmapOverlay))
                    val videoEffects: List<Effect> = listOf(overlayEffect)
                    val effects = Effects(
                        /* audioProcessors= */ emptyList(),
                        /* videoEffects= */ videoEffects
                    )

                    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                        .setEffects(effects)
                        .build()

                    // 2. 对于单个视频，直接开始转换EditedMediaItem
                    transformer.start(editedMediaItem, outputVideoFile.absolutePath)

                    continuation.invokeOnCancellation {
                        transformer.cancel()
                    }
                }
            }

            // 根据参数决定是否释放缩放后的水印资源
            if (recycleWatermark) {
                scaledWatermarkBitmap.recycle()
            }

            return@withContext result
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
    @UnstableApi
    suspend fun addWatermarkToVideo(
        context: Context,
        inputVideoFile: File,
        watermarkUrl: String,
        outputVideoFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.1f,
        marginFactor: Float = 0.03f,
        alpha: Float = 1f
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取或加载水印位图
            val watermarkBitmap = getOrLoadWatermarkBitmap(context, watermarkUrl)
            
            val result = addWatermarkToVideo(
                context,
                inputVideoFile,
                watermarkBitmap,
                outputVideoFile,
                position,
                sizeFactor,
                marginFactor,
                alpha,
                false // 不回收水印，因为它被缓存了
            )
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
    @UnstableApi
    suspend fun addWatermarkToVideoFromFile(
        context: Context,
        inputVideoFile: File,
        watermarkFile: File,
        outputVideoFile: File,
        position: WatermarkPosition = WatermarkPosition.RIGHT_BOTTOM,
        sizeFactor: Float = 0.1f,
        marginFactor: Float = 0.03f,
        alpha: Float = 1f
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 使用文件路径作为缓存key
            val filePath = watermarkFile.absolutePath
            
            // 从缓存获取或加载水印
            val watermarkBitmap = watermarkBitmapCache[filePath] ?: run {
                val bitmap = BitmapFactory.decodeFile(filePath)
                watermarkBitmapCache[filePath] = bitmap
                bitmap
            }
            
            val result = addWatermarkToVideo(
                context,
                inputVideoFile,
                watermarkBitmap,
                outputVideoFile,
                position,
                sizeFactor,
                marginFactor,
                alpha,
                false // 不回收水印，因为它被缓存了
            )
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "添加视频水印失败", e)
            return@withContext false
        }
    }
    
    /**
     * 清除所有水印缓存
     */
    fun clearWatermarkCache() {
        watermarkBitmapCache.values.forEach { it.recycle() }
        watermarkBitmapCache.clear()
    }
    
    /**
     * 清除指定URL的水印缓存
     */
    fun clearWatermarkCache(url: String) {
        watermarkBitmapCache[url]?.recycle()
        watermarkBitmapCache.remove(url)
    }
}