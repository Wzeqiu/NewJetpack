package com.common.widget.view.imgCrop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 图片裁剪工具类
 * 提供图片裁剪、旋转、缩放等功能
 */
object ImageCropUtils {

    private const val TAG = "ImageCropUtils"
    private const val JPEG_QUALITY = 90
    private const val MAX_BITMAP_SIZE = 4096

    /**
     * 裁剪图片
     */
    suspend fun cropImage(
        context: Context,
        sourceUri: Uri,
        cropRect: RectF,
        outputFile: File,
        quality: Int = JPEG_QUALITY
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadBitmapFromUri(context, sourceUri) ?: return@withContext false
            val croppedBitmap = cropBitmap(bitmap, cropRect) ?: return@withContext false

            saveBitmapToFile(croppedBitmap, outputFile, quality)
        } catch (e: Exception) {
            Log.e(TAG, "裁剪图片失败", e)
            false
        }
    }

    /**
     * 裁剪位图
     */
    fun cropBitmap(source: Bitmap, cropRect: RectF): Bitmap? {
        return try {
            val left = max(0f, cropRect.left).toInt()
            val top = max(0f, cropRect.top).toInt()
            val right = min(source.width.toFloat(), cropRect.right).toInt()
            val bottom = min(source.height.toFloat(), cropRect.bottom).toInt()

            if (left >= right || top >= bottom) return null

            Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        } catch (e: Exception) {
            Log.e(TAG, "裁剪位图失败", e)
            null
        }
    }

    /**
     * 圆形裁剪
     */
    fun cropToCircle(source: Bitmap): Bitmap? {
        return try {
            val size = min(source.width, source.height)
            val radius = size / 2f
            val centerX = source.width / 2f
            val centerY = source.height / 2f

            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint().apply {
                isAntiAlias = true
            }

            // 绘制圆形遮罩
            canvas.drawCircle(radius, radius, radius, paint)

            // 设置混合模式
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            // 绘制原图
            val left = centerX - radius
            val top = centerY - radius
            canvas.drawBitmap(source, -left, -top, paint)

            output
        } catch (e: Exception) {
            Log.e(TAG, "圆形裁剪失败", e)
            null
        }
    }

    /**
     * 圆角矩形裁剪
     */
    fun cropToRoundedRect(source: Bitmap, cornerRadius: Float): Bitmap? {
        return try {
            val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            val paint = Paint().apply {
                isAntiAlias = true
            }

            val rect = RectF(0f, 0f, source.width.toFloat(), source.height.toFloat())

            // 绘制圆角矩形遮罩
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

            // 设置混合模式
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

            // 绘制原图
            canvas.drawBitmap(source, 0f, 0f, paint)

            output
        } catch (e: Exception) {
            Log.e(TAG, "圆角矩形裁剪失败", e)
            null
        }
    }

    /**
     * 按比例裁剪
     */
    fun cropByAspectRatio(source: Bitmap, aspectRatio: Float): Bitmap? {
        return try {
            val sourceWidth = source.width.toFloat()
            val sourceHeight = source.height.toFloat()
            val sourceRatio = sourceWidth / sourceHeight

            val cropRect = if (sourceRatio > aspectRatio) {
                // 原图更宽，需要裁剪宽度
                val cropWidth = sourceHeight * aspectRatio
                val left = (sourceWidth - cropWidth) / 2
                RectF(left, 0f, left + cropWidth, sourceHeight)
            } else {
                // 原图更高，需要裁剪高度
                val cropHeight = sourceWidth / aspectRatio
                val top = (sourceHeight - cropHeight) / 2
                RectF(0f, top, sourceWidth, top + cropHeight)
            }

            cropBitmap(source, cropRect)
        } catch (e: Exception) {
            Log.e(TAG, "按比例裁剪失败", e)
            null
        }
    }

    /**
     * 智能裁剪 - 保持长宽比的中心裁剪
     */
    fun smartCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            val sourceWidth = source.width.toFloat()
            val sourceHeight = source.height.toFloat()
            val targetRatio = targetWidth.toFloat() / targetHeight
            val sourceRatio = sourceWidth / sourceHeight

            val scale: Float
            val cropRect: RectF

            if (sourceRatio > targetRatio) {
                // 原图更宽，以高度为准缩放
                scale = targetHeight / sourceHeight
                val scaledWidth = sourceWidth * scale
                val left = (scaledWidth - targetWidth) / 2 / scale
                cropRect = RectF(left, 0f, left + targetWidth / scale, sourceHeight)
            } else {
                // 原图更高，以宽度为准缩放
                scale = targetWidth / sourceWidth
                val scaledHeight = sourceHeight * scale
                val top = (scaledHeight - targetHeight) / 2 / scale
                cropRect = RectF(0f, top, sourceWidth, top + targetHeight / scale)
            }

            val croppedBitmap = cropBitmap(source, cropRect) ?: return null
            Bitmap.createScaledBitmap(croppedBitmap, targetWidth, targetHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "智能裁剪失败", e)
            null
        }
    }

    /**
     * 旋转图片
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap? {
        return try {
            if (degrees == 0f) return source

            val matrix = Matrix().apply {
                postRotate(degrees)
            }

            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "旋转图片失败", e)
            null
        }
    }

    /**
     * 翻转图片
     */
    fun flipBitmap(source: Bitmap, horizontal: Boolean = true, vertical: Boolean = false): Bitmap? {
        return try {
            val matrix = Matrix().apply {
                if (horizontal) {
                    postScale(-1f, 1f)
                }
                if (vertical) {
                    postScale(1f, -1f)
                }
            }

            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "翻转图片失败", e)
            null
        }
    }

    /**
     * 从URI加载位图
     */
    private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream =
                    context.contentResolver.openInputStream(uri) ?: return@withContext null

                // 先获取图片尺寸
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                // 计算缩放比例
                val sampleSize = calculateInSampleSize(options, MAX_BITMAP_SIZE, MAX_BITMAP_SIZE)

                // 加载图片
                val newInputStream =
                    context.contentResolver.openInputStream(uri) ?: return@withContext null
                options.apply {
                    inJustDecodeBounds = false
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
                newInputStream.close()

                // 处理旋转
                bitmap?.let { correctBitmapOrientation(context, uri, it) }
            } catch (e: Exception) {
                Log.e(TAG, "从URI加载位图失败", e)
                null
            }
        }

    /**
     * 计算采样率
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * 修正图片方向
     */
    private suspend fun correctBitmapOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap =
        withContext(Dispatchers.IO) {
            try {
                val inputStream =
                    context.contentResolver.openInputStream(uri) ?: return@withContext bitmap
                val exif = ExifInterface(inputStream)
                inputStream.close()

                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val degrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                if (degrees != 0f) {
                    rotateBitmap(bitmap, degrees) ?: bitmap
                } else {
                    bitmap
                }
            } catch (e: Exception) {
                Log.e(TAG, "修正图片方向失败", e)
                bitmap
            }
        }

    /**
     * 保存位图到文件
     */
    suspend fun saveBitmapToFile(bitmap: Bitmap, file: File, quality: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // 确保父目录存在
                file.parentFile?.mkdirs()

                val outputStream = FileOutputStream(file)
                val format = if (file.extension.lowercase() == "png") {
                    Bitmap.CompressFormat.PNG
                } else {
                    Bitmap.CompressFormat.JPEG
                }

                val success = bitmap.compress(format, quality, outputStream)
                outputStream.close()

                success
            } catch (e: Exception) {
                Log.e(TAG, "保存位图到文件失败", e)
                false
            }
        }

    /**
     * 计算裁剪区域相对于原图的位置
     */
    fun calculateCropRect(
        imageWidth: Int,
        imageHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
        cropRect: RectF
    ): RectF {
        // 计算图片在显示区域中的实际位置和缩放比例
        val scale = min(displayWidth.toFloat() / imageWidth, displayHeight.toFloat() / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale

        val offsetX = (displayWidth - scaledWidth) / 2
        val offsetY = (displayHeight - scaledHeight) / 2

        // 将显示区域的裁剪坐标转换为原图坐标
        val originalLeft = (cropRect.left - offsetX) / scale
        val originalTop = (cropRect.top - offsetY) / scale
        val originalRight = (cropRect.right - offsetX) / scale
        val originalBottom = (cropRect.bottom - offsetY) / scale

        return RectF(originalLeft, originalTop, originalRight, originalBottom)
    }
}