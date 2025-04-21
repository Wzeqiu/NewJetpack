package com.wzeqiu.newjetpack.view

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import com.wzeqiu.newjetpack.R

/**
 * 自定义View Shape属性帮助类
 * 用于处理自定义View的shape属性，包括圆角、描边、渐变背景等
 * 可以被不同的自定义View复用，提高代码复用性
 */
class ShapeViewHelper(private val view: View) {
    
    // 形状类型
    private var shapeType = GradientDrawable.RECTANGLE
    
    // 背景颜色
    private var backgroundColor = Color.TRANSPARENT
    
    // 边框颜色
    private var strokeColor = Color.TRANSPARENT
    
    // 边框宽度
    private var strokeWidth = 0f
    
    // 圆角半径
    private var radius = 0f
    private var topLeftRadius = 0f
    private var topRightRadius = 0f
    private var bottomLeftRadius = 0f
    private var bottomRightRadius = 0f
    
    // 渐变相关属性
    private var gradientStartColor = Color.TRANSPARENT
    private var gradientCenterColor = Color.TRANSPARENT
    private var gradientEndColor = Color.TRANSPARENT
    private var gradientOrientation = GradientDrawable.Orientation.TOP_BOTTOM
    
    // 是否使用渐变
    private var useGradient = false
    
    /**
     * 从AttributeSet中解析自定义属性
     * @param context 上下文
     * @param attrs 属性集
     * @param defStyleAttr 默认样式属性
     */
    fun parseAttributes(attrs: AttributeSet?, defStyleAttr: Int, styleableResId: IntArray) {
        if (attrs == null) return
        val typedArray: TypedArray = view.context.obtainStyledAttributes(attrs, styleableResId, defStyleAttr, 0)
        try {
            // 解析背景颜色
            backgroundColor = typedArray.getColor(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_background_color)),
                Color.TRANSPARENT
            )
            
            // 解析边框属性
            strokeColor = typedArray.getColor(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_stroke_color)),
                Color.TRANSPARENT
            )
            strokeWidth = typedArray.getDimension(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_stroke_width)),
                0f
            )
            
            // 解析圆角属性
            radius = typedArray.getDimension(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_radius)),
                0f
            )
            
            // 如果设置了单独的圆角，则使用单独的圆角值
            topLeftRadius = typedArray.getDimension(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_top_left_radius)),
                radius
            )
            topRightRadius = typedArray.getDimension(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_top_right_radius)),
                radius
            )
            bottomLeftRadius = typedArray.getDimension(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_bottom_left_radius)),
                radius
            )
            bottomRightRadius = typedArray.getDimension(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_bottom_right_radius)),
                radius
            )
            
            // 解析渐变属性
            gradientStartColor = typedArray.getColor(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_gradient_start_color)),
                Color.TRANSPARENT
            )
            gradientCenterColor = typedArray.getColor(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_gradient_center_color)),
                Color.TRANSPARENT
            )
            gradientEndColor = typedArray.getColor(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_gradient_end_color)),
                Color.TRANSPARENT
            )
            
            // 判断是否使用渐变
            useGradient = gradientStartColor != Color.TRANSPARENT && gradientEndColor != Color.TRANSPARENT
            
            // 解析渐变方向
            val orientationValue = typedArray.getInt(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_gradient_orientation)),
                0
            )
            gradientOrientation = when (orientationValue) {
                0 -> GradientDrawable.Orientation.TOP_BOTTOM
                1 -> GradientDrawable.Orientation.BOTTOM_TOP
                2 -> GradientDrawable.Orientation.LEFT_RIGHT
                3 -> GradientDrawable.Orientation.RIGHT_LEFT
                4 -> GradientDrawable.Orientation.TL_BR
                5 -> GradientDrawable.Orientation.BR_TL
                6 -> GradientDrawable.Orientation.TR_BL
                7 -> GradientDrawable.Orientation.BL_TR
                else -> GradientDrawable.Orientation.TOP_BOTTOM
            }
            
            // 解析形状类型
            shapeType = typedArray.getInt(
                typedArray.getIndex(styleableResId.indexOf(R.styleable.ShapeViewCommon_shape_type)),
                GradientDrawable.RECTANGLE
            )
            
        } finally {
            typedArray.recycle()
        }
    }
    
    /**
     * 应用shape属性到View
     */
    fun applyBackground() {
        val drawable = GradientDrawable()
        drawable.shape = shapeType
        
        // 设置圆角
        if (radius > 0 || topLeftRadius > 0 || topRightRadius > 0 || bottomLeftRadius > 0 || bottomRightRadius > 0) {
            // 如果四个角的圆角值不同，则分别设置
            if (topLeftRadius != radius || topRightRadius != radius || bottomLeftRadius != radius || bottomRightRadius != radius) {
                drawable.cornerRadii = floatArrayOf(
                    topLeftRadius, topLeftRadius,
                    topRightRadius, topRightRadius,
                    bottomRightRadius, bottomRightRadius,
                    bottomLeftRadius, bottomLeftRadius
                )
            } else {
                drawable.cornerRadius = radius
            }
        }
        
        // 设置边框
        if (strokeWidth > 0 && strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(strokeWidth.toInt(), strokeColor)
        }
        
        // 设置背景颜色或渐变
        if (useGradient) {
            // 使用渐变
            val colors = if (gradientCenterColor != Color.TRANSPARENT) {
                intArrayOf(gradientStartColor, gradientCenterColor, gradientEndColor)
            } else {
                intArrayOf(gradientStartColor, gradientEndColor)
            }
            drawable.orientation = gradientOrientation
            drawable.colors = colors
        } else {
            // 使用纯色背景
            drawable.setColor(backgroundColor)
        }
        
        // 应用背景
        view.background = drawable
    }
    
    /**
     * 动态设置背景颜色
     * @param color 颜色值
     */
    fun setBackgroundColor(color: Int) {
        this.backgroundColor = color
        this.useGradient = false
        applyBackground()
    }
    
    /**
     * 动态设置边框颜色和宽度
     * @param color 边框颜色
     * @param width 边框宽度（px）
     */
    fun setStroke(color: Int, width: Float) {
        this.strokeColor = color
        this.strokeWidth = width
        applyBackground()
    }
    
    /**
     * 动态设置圆角半径
     * @param radius 圆角半径（px）
     */
    fun setRadius(radius: Float) {
        this.radius = radius
        this.topLeftRadius = radius
        this.topRightRadius = radius
        this.bottomLeftRadius = radius
        this.bottomRightRadius = radius
        applyBackground()
    }
    
    /**
     * 动态设置不同角的圆角半径
     * @param topLeft 左上角圆角半径
     * @param topRight 右上角圆角半径
     * @param bottomLeft 左下角圆角半径
     * @param bottomRight 右下角圆角半径
     */
    fun setCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        this.topLeftRadius = topLeft
        this.topRightRadius = topRight
        this.bottomLeftRadius = bottomLeft
        this.bottomRightRadius = bottomRight
        applyBackground()
    }
    
    /**
     * 动态设置渐变背景
     * @param startColor 开始颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setGradientBackground(startColor: Int, endColor: Int, orientation: GradientDrawable.Orientation) {
        this.gradientStartColor = startColor
        this.gradientEndColor = endColor
        this.gradientOrientation = orientation
        this.useGradient = true
        applyBackground()
    }
    
    /**
     * 动态设置渐变背景（带中间颜色）
     * @param startColor 开始颜色
     * @param centerColor 中间颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setGradientBackground(startColor: Int, centerColor: Int, endColor: Int, orientation: GradientDrawable.Orientation) {
        this.gradientStartColor = startColor
        this.gradientCenterColor = centerColor
        this.gradientEndColor = endColor
        this.gradientOrientation = orientation
        this.useGradient = true
        applyBackground()
    }
    
    /**
     * 动态设置形状类型
     * @param shapeType 形状类型
     */
    fun setShapeType(shapeType: Int) {
        this.shapeType = shapeType
        applyBackground()
    }
}