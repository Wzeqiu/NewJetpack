package com.common.widget.view.imgCrop

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.*

/**
 * 图片裁剪视图
 * 支持拖动、缩放裁剪框实现图片的裁剪
 */
class ImageCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val CORNER_SIZE = 60f
        private const val MIN_CROP_SIZE = 100f
        private const val TOUCH_TOLERANCE = 50f
        
        // 常见长宽比
        const val ASPECT_RATIO_FREE = 0f      // 自由比例
        const val ASPECT_RATIO_1_1 = 1f       // 正方形 1:1
        const val ASPECT_RATIO_4_3 = 4f / 3f  // 4:3
        const val ASPECT_RATIO_3_4 = 3f / 4f  // 3:4
        const val ASPECT_RATIO_16_9 = 16f / 9f // 16:9
        const val ASPECT_RATIO_9_16 = 9f / 16f // 9:16
        const val ASPECT_RATIO_3_2 = 3f / 2f  // 3:2
        const val ASPECT_RATIO_2_3 = 2f / 3f  // 2:3
        const val ASPECT_RATIO_5_4 = 5f / 4f  // 5:4
        const val ASPECT_RATIO_4_5 = 4f / 5f  // 4:5
    }

    // 图片相关
    private var originalBitmap: Bitmap? = null
    private val imageMatrix = Matrix()
    private val imageRect = RectF()

    // 裁剪框相关
    private val cropRect = RectF()
    private var isDragging = false
    private var isResizing = false
    private var resizeCorner = -1 // 0:左上 1:右上 2:右下 3:左下
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // 长宽比配置
    private var aspectRatio = ASPECT_RATIO_FREE // 当前长宽比，0表示自由比例
    private var isAspectRatioLocked = false // 是否锁定长宽比

    // 画笔
    private val cropPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val maskPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#A2FF68")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#66FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    // 回调接口
    interface OnCropChangeListener {
        fun onCropRectChanged(cropRect: RectF)
    }

    private var cropChangeListener: OnCropChangeListener? = null

    fun setOnCropChangeListener(listener: OnCropChangeListener?) {
        this.cropChangeListener = listener
    }

    /**
     * 设置要裁剪的图片
     */
    fun setImageBitmap(bitmap: Bitmap?) {
        originalBitmap = bitmap
        if (bitmap != null) {
            setupImage()
        }
        invalidate()
    }

    /**
     * 设置图片资源
     */
    fun setImageResource(resId: Int) {
        val drawable = context.getDrawable(resId)
        drawable?.let {
            setImageBitmap(it.toBitmap())
        }
    }

    /**
     * 获取裁剪后的图片
     */
    fun getCroppedBitmap(): Bitmap? {
        val bitmap = originalBitmap ?: return null
        
        // 计算原图对应的裁剪区域
        val imageInverseMatrix = Matrix()
        if (!imageMatrix.invert(imageInverseMatrix)) return null
        
        val originalCropRect = RectF()
        imageInverseMatrix.mapRect(originalCropRect, cropRect)
        
        // 确保裁剪区域在图片范围内
        val left = max(0f, originalCropRect.left).toInt()
        val top = max(0f, originalCropRect.top).toInt()
        val right = min(bitmap.width.toFloat(), originalCropRect.right).toInt()
        val bottom = min(bitmap.height.toFloat(), originalCropRect.bottom).toInt()
        
        if (left >= right || top >= bottom) return null
        
        return try {
            Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 设置长宽比
     * @param ratio 长宽比，使用预设的常量或自定义值，0表示自由比例
     */
    fun setAspectRatio(ratio: Float) {
        aspectRatio = ratio
        isAspectRatioLocked = ratio != ASPECT_RATIO_FREE
        applyCropRectWithAspectRatio()
    }
    
    /**
     * 获取当前长宽比
     */
    fun getAspectRatio(): Float = aspectRatio
    
    /**
     * 是否锁定长宽比
     */
    fun isAspectRatioLocked(): Boolean = isAspectRatioLocked
    
    /**
     * 设置是否锁定长宽比
     */
    fun setAspectRatioLocked(locked: Boolean) {
        isAspectRatioLocked = locked
        if (locked && aspectRatio != ASPECT_RATIO_FREE) {
            applyCropRectWithAspectRatio()
        }
    }
    
    /**
     * 应用长宽比到当前裁剪区域
     */
    private fun applyCropRectWithAspectRatio() {
        if (imageRect.isEmpty || aspectRatio == ASPECT_RATIO_FREE) return
        
        val centerX = cropRect.centerX()
        val centerY = cropRect.centerY()
        val currentWidth = cropRect.width()
        val currentHeight = cropRect.height()
        
        val newWidth: Float
        val newHeight: Float
        
        if (currentWidth / currentHeight > aspectRatio) {
            // 当前更宽，以高度为准
            newHeight = currentHeight
            newWidth = newHeight * aspectRatio
        } else {
            // 当前更高，以宽度为准
            newWidth = currentWidth
            newHeight = newWidth / aspectRatio
        }
        
        // 确保新尺寸不超出图片边界
        val maxWidth = imageRect.width()
        val maxHeight = imageRect.height()
        
        val finalWidth = min(newWidth, maxWidth)
        val finalHeight = min(newHeight, maxHeight)
        
        // 重新计算以保持比例
        val adjustedWidth: Float
        val adjustedHeight: Float
        
        if (finalWidth / finalHeight > aspectRatio) {
            adjustedHeight = finalHeight
            adjustedWidth = adjustedHeight * aspectRatio
        } else {
            adjustedWidth = finalWidth
            adjustedHeight = adjustedWidth / aspectRatio
        }
        
        val left = centerX - adjustedWidth / 2
        val top = centerY - adjustedHeight / 2
        val right = left + adjustedWidth
        val bottom = top + adjustedHeight
        
        // 确保不超出图片边界
        if (left >= imageRect.left && top >= imageRect.top &&
            right <= imageRect.right && bottom <= imageRect.bottom) {
            cropRect.set(left, top, right, bottom)
        } else {
            // 如果超出边界，重新计算位置
            resetCropRectWithAspectRatio()
        }
        
        cropChangeListener?.onCropRectChanged(cropRect)
        invalidate()
    }
    
    /**
     * 重置裁剪区域到默认位置
     */
    fun resetCropRect() {
        if (aspectRatio != ASPECT_RATIO_FREE) {
            resetCropRectWithAspectRatio()
        } else {
            resetCropRectFree()
        }
    }
    
    /**
     * 重置为自由比例的裁剪区域
     */
    private fun resetCropRectFree() {
        if (imageRect.isEmpty) return
        
        val size = min(imageRect.width(), imageRect.height()) * 0.8f
        val centerX = imageRect.centerX()
        val centerY = imageRect.centerY()
        
        cropRect.set(
            centerX - size / 2,
            centerY - size / 2,
            centerX + size / 2,
            centerY + size / 2
        )
        
        cropChangeListener?.onCropRectChanged(cropRect)
        invalidate()
    }
    
    /**
     * 重置为指定长宽比的裁剪区域
     */
    private fun resetCropRectWithAspectRatio() {
        if (imageRect.isEmpty || aspectRatio == ASPECT_RATIO_FREE) return
        
        val imageWidth = imageRect.width()
        val imageHeight = imageRect.height()
        val imageAspectRatio = imageWidth / imageHeight
        
        val cropWidth: Float
        val cropHeight: Float
        
        if (imageAspectRatio > aspectRatio) {
            // 图片更宽，以高度为基准
            cropHeight = imageHeight * 0.8f
            cropWidth = cropHeight * aspectRatio
        } else {
            // 图片更高，以宽度为基准
            cropWidth = imageWidth * 0.8f
            cropHeight = cropWidth / aspectRatio
        }
        
        val centerX = imageRect.centerX()
        val centerY = imageRect.centerY()
        
        cropRect.set(
            centerX - cropWidth / 2,
            centerY - cropHeight / 2,
            centerX + cropWidth / 2,
            centerY + cropHeight / 2
        )
        
        cropChangeListener?.onCropRectChanged(cropRect)
        invalidate()
    }

    private fun setupImage() {
        val bitmap = originalBitmap ?: return
        
        if (width == 0 || height == 0) {
            // View 还没有测量完成，等待下次 onSizeChanged
            return
        }
        
        // 计算图片显示矩阵
        val imageWidth = bitmap.width.toFloat()
        val imageHeight = bitmap.height.toFloat()
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        val scale = min(viewWidth / imageWidth, viewHeight / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        
        val left = (viewWidth - scaledWidth) / 2
        val top = (viewHeight - scaledHeight) / 2
        
        imageMatrix.reset()
        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(left, top)
        
        imageRect.set(left, top, left + scaledWidth, top + scaledHeight)
        
        // 初始化裁剪区域
        resetCropRect()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (originalBitmap != null) {
            setupImage()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val bitmap = originalBitmap ?: return
        
        // 绘制图片
        canvas.drawBitmap(bitmap, imageMatrix, null)
        
        // 绘制遮罩
        drawMask(canvas)
        
        // 绘制裁剪框
        drawCropFrame(canvas)
        
        // 绘制网格线
        drawGrid(canvas)
        
        // 绘制角点
        drawCorners(canvas)
    }

    private fun drawMask(canvas: Canvas) {
        // 绘制四个遮罩区域
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, maskPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), maskPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, maskPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, maskPaint)
    }

    private fun drawCropFrame(canvas: Canvas) {
        canvas.drawRect(cropRect, cropPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        val width = cropRect.width() / 3
        val height = cropRect.height() / 3
        
        // 垂直线
        for (i in 1..2) {
            val x = cropRect.left + width * i
            canvas.drawLine(x, cropRect.top, x, cropRect.bottom, gridPaint)
        }
        
        // 水平线
        for (i in 1..2) {
            val y = cropRect.top + height * i
            canvas.drawLine(cropRect.left, y, cropRect.right, y, gridPaint)
        }
    }

    private fun drawCorners(canvas: Canvas) {
        val cornerLength = CORNER_SIZE / 2
        
        // 左上角
        canvas.drawLine(
            cropRect.left, cropRect.top,
            cropRect.left + cornerLength, cropRect.top,
            cornerPaint
        )
        canvas.drawLine(
            cropRect.left, cropRect.top,
            cropRect.left, cropRect.top + cornerLength,
            cornerPaint
        )
        
        // 右上角
        canvas.drawLine(
            cropRect.right, cropRect.top,
            cropRect.right - cornerLength, cropRect.top,
            cornerPaint
        )
        canvas.drawLine(
            cropRect.right, cropRect.top,
            cropRect.right, cropRect.top + cornerLength,
            cornerPaint
        )
        
        // 右下角
        canvas.drawLine(
            cropRect.right, cropRect.bottom,
            cropRect.right - cornerLength, cropRect.bottom,
            cornerPaint
        )
        canvas.drawLine(
            cropRect.right, cropRect.bottom,
            cropRect.right, cropRect.bottom - cornerLength,
            cornerPaint
        )
        
        // 左下角
        canvas.drawLine(
            cropRect.left, cropRect.bottom,
            cropRect.left + cornerLength, cropRect.bottom,
            cornerPaint
        )
        canvas.drawLine(
            cropRect.left, cropRect.bottom,
            cropRect.left, cropRect.bottom - cornerLength,
            cornerPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = x
                lastTouchY = y
                
                // 检查是否点击了角点
                resizeCorner = getCornerAtPosition(x, y)
                if (resizeCorner != -1) {
                    isResizing = true
                    return true
                }
                
                // 检查是否点击了裁剪框内部
                if (cropRect.contains(x, y)) {
                    isDragging = true
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - lastTouchX
                val deltaY = y - lastTouchY
                
                if (isResizing) {
                    resizeCropRect(deltaX, deltaY)
                    invalidate()
                    cropChangeListener?.onCropRectChanged(cropRect)
                } else if (isDragging) {
                    moveCropRect(deltaX, deltaY)
                    invalidate()
                    cropChangeListener?.onCropRectChanged(cropRect)
                }
                
                lastTouchX = x
                lastTouchY = y
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isResizing = false
                resizeCorner = -1
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }

    private fun getCornerAtPosition(x: Float, y: Float): Int {
        val tolerance = TOUCH_TOLERANCE
        
        // 检查四个角点
        val corners = arrayOf(
            PointF(cropRect.left, cropRect.top),      // 0: 左上
            PointF(cropRect.right, cropRect.top),     // 1: 右上
            PointF(cropRect.right, cropRect.bottom),  // 2: 右下
            PointF(cropRect.left, cropRect.bottom)    // 3: 左下
        )
        
        for (i in corners.indices) {
            val corner = corners[i]
            if (abs(x - corner.x) <= tolerance && abs(y - corner.y) <= tolerance) {
                return i
            }
        }
        
        return -1
    }

    private fun resizeCropRect(deltaX: Float, deltaY: Float) {
        if (isAspectRatioLocked && aspectRatio != ASPECT_RATIO_FREE) {
            resizeCropRectWithAspectRatio(deltaX, deltaY)
        } else {
            resizeCropRectFree(deltaX, deltaY)
        }
    }
    
    /**
     * 自由比例调整裁剪框大小
     */
    private fun resizeCropRectFree(deltaX: Float, deltaY: Float) {
        val newRect = RectF(cropRect)
        
        when (resizeCorner) {
            0 -> { // 左上角
                newRect.left += deltaX
                newRect.top += deltaY
            }
            1 -> { // 右上角
                newRect.right += deltaX
                newRect.top += deltaY
            }
            2 -> { // 右下角
                newRect.right += deltaX
                newRect.bottom += deltaY
            }
            3 -> { // 左下角
                newRect.left += deltaX
                newRect.bottom += deltaY
            }
        }
        
        // 确保最小尺寸
        if (newRect.width() >= MIN_CROP_SIZE && newRect.height() >= MIN_CROP_SIZE) {
            // 确保不超出图片边界
            if (newRect.left >= imageRect.left && newRect.top >= imageRect.top &&
                newRect.right <= imageRect.right && newRect.bottom <= imageRect.bottom) {
                cropRect.set(newRect)
            }
        }
    }
    
    /**
     * 锁定长宽比调整裁剪框大小
     */
    private fun resizeCropRectWithAspectRatio(deltaX: Float, deltaY: Float) {

        // 根据拖拽的角点和移动距离计算新的尺寸
        val newWidth: Float
        val newHeight: Float
        
        when (resizeCorner) {
            0 -> { // 左上角
                // 以对角线距离来计算缩放
                val centerX = cropRect.right
                val centerY = cropRect.bottom
                val oldDistance = sqrt((cropRect.right - cropRect.left).pow(2) + (cropRect.bottom - cropRect.top).pow(2))
                val newX = cropRect.left + deltaX
                val newY = cropRect.top + deltaY
                val newDistance = sqrt((centerX - newX).pow(2) + (centerY - newY).pow(2))
                val scale = newDistance / oldDistance
                
                newWidth = cropRect.width() * scale
                newHeight = newWidth / aspectRatio
            }
            1 -> { // 右上角
                val centerX = cropRect.left
                val centerY = cropRect.bottom
                val oldDistance = sqrt((cropRect.right - cropRect.left).pow(2) + (cropRect.bottom - cropRect.top).pow(2))
                val newX = cropRect.right + deltaX
                val newY = cropRect.top + deltaY
                val newDistance = sqrt((newX - centerX).pow(2) + (centerY - newY).pow(2))
                val scale = newDistance / oldDistance
                
                newWidth = cropRect.width() * scale
                newHeight = newWidth / aspectRatio
            }
            2 -> { // 右下角
                val centerX = cropRect.left
                val centerY = cropRect.top
                val oldDistance = sqrt((cropRect.right - cropRect.left).pow(2) + (cropRect.bottom - cropRect.top).pow(2))
                val newX = cropRect.right + deltaX
                val newY = cropRect.bottom + deltaY
                val newDistance = sqrt((newX - centerX).pow(2) + (newY - centerY).pow(2))
                val scale = newDistance / oldDistance
                
                newWidth = cropRect.width() * scale
                newHeight = newWidth / aspectRatio
            }
            3 -> { // 左下角
                val centerX = cropRect.right
                val centerY = cropRect.top
                val oldDistance = sqrt((cropRect.right - cropRect.left).pow(2) + (cropRect.bottom - cropRect.top).pow(2))
                val newX = cropRect.left + deltaX
                val newY = cropRect.bottom + deltaY
                val newDistance = sqrt((centerX - newX).pow(2) + (newY - centerY).pow(2))
                val scale = newDistance / oldDistance
                
                newWidth = cropRect.width() * scale
                newHeight = newWidth / aspectRatio
            }
            else -> return
        }
        
        // 确保最小尺寸
        if (newWidth < MIN_CROP_SIZE || newHeight < MIN_CROP_SIZE) return
        
        // 计算新的裁剪框位置
        val newRect = RectF()
        when (resizeCorner) {
            0 -> { // 左上角固定右下角
                newRect.right = cropRect.right
                newRect.bottom = cropRect.bottom
                newRect.left = newRect.right - newWidth
                newRect.top = newRect.bottom - newHeight
            }
            1 -> { // 右上角固定左下角
                newRect.left = cropRect.left
                newRect.bottom = cropRect.bottom
                newRect.right = newRect.left + newWidth
                newRect.top = newRect.bottom - newHeight
            }
            2 -> { // 右下角固定左上角
                newRect.left = cropRect.left
                newRect.top = cropRect.top
                newRect.right = newRect.left + newWidth
                newRect.bottom = newRect.top + newHeight
            }
            3 -> { // 左下角固定右上角
                newRect.right = cropRect.right
                newRect.top = cropRect.top
                newRect.left = newRect.right - newWidth
                newRect.bottom = newRect.top + newHeight
            }
        }
        
        // 确保不超出图片边界
        if (newRect.left >= imageRect.left && newRect.top >= imageRect.top &&
            newRect.right <= imageRect.right && newRect.bottom <= imageRect.bottom) {
            cropRect.set(newRect)
        }
    }

    private fun moveCropRect(deltaX: Float, deltaY: Float) {
        val newRect = RectF(cropRect)
        newRect.offset(deltaX, deltaY)
        
        // 确保不超出图片边界
        if (newRect.left >= imageRect.left && newRect.top >= imageRect.top &&
            newRect.right <= imageRect.right && newRect.bottom <= imageRect.bottom) {
            cropRect.set(newRect)
        }
    }
}