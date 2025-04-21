package com.wzeqiu.newjetpack.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatEditText
import com.wzeqiu.newjetpack.R

/**
 * 自定义LinearLayout，支持shape属性
 * 支持圆角、描边、渐变背景等特性
 * 可通过XML属性或代码动态设置各种shape属性
 */
class ShapeLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // 使用ShapeViewHelper处理shape相关属性
    private val shapeHelper: ShapeViewHelper = ShapeViewHelper(this)

    init {
        // 解析自定义属性
        shapeHelper.parseAttributes( attrs, defStyleAttr, R.styleable.ShapeLinearLayout)
        // 应用背景
        shapeHelper.applyBackground()
    }

    /**
     * 设置背景颜色
     * @param color 颜色值
     */
    fun setShapeBackgroundColor(color: Int) {
        shapeHelper.setBackgroundColor(color)
    }

    /**
     * 设置边框
     * @param color 边框颜色
     * @param width 边框宽度（px）
     */
    fun setShapeStroke(color: Int, width: Float) {
        shapeHelper.setStroke(color, width)
    }

    /**
     * 设置圆角半径
     * @param radius 圆角半径（px）
     */
    fun setShapeRadius(radius: Float) {
        shapeHelper.setRadius(radius)
    }

    /**
     * 设置不同角的圆角半径
     * @param topLeft 左上角圆角半径
     * @param topRight 右上角圆角半径
     * @param bottomLeft 左下角圆角半径
     * @param bottomRight 右下角圆角半径
     */
    fun setShapeCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        shapeHelper.setCornerRadii(topLeft, topRight, bottomLeft, bottomRight)
    }

    /**
     * 设置渐变背景
     * @param startColor 开始颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setShapeGradientBackground(startColor: Int, endColor: Int, orientation: android.graphics.drawable.GradientDrawable.Orientation) {
        shapeHelper.setGradientBackground(startColor, endColor, orientation)
    }

    /**
     * 设置渐变背景（带中间颜色）
     * @param startColor 开始颜色
     * @param centerColor 中间颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setShapeGradientBackground(startColor: Int, centerColor: Int, endColor: Int, orientation: android.graphics.drawable.GradientDrawable.Orientation) {
        shapeHelper.setGradientBackground(startColor, centerColor, endColor, orientation)
    }
}

/**
 * 自定义EditText，支持shape属性
 * 支持圆角、描边、渐变背景等特性
 * 可通过XML属性或代码动态设置各种shape属性
 */
class ShapeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    // 使用ShapeViewHelper处理shape相关属性
    private val shapeHelper: ShapeViewHelper = ShapeViewHelper(this)

    init {
        // 解析自定义属性
        shapeHelper.parseAttributes( attrs, defStyleAttr, R.styleable.ShapeEditText)
        // 应用背景
        shapeHelper.applyBackground()
    }

    /**
     * 设置背景颜色
     * @param color 颜色值
     */
    fun setShapeBackgroundColor(color: Int) {
        shapeHelper.setBackgroundColor(color)
    }

    /**
     * 设置边框
     * @param color 边框颜色
     * @param width 边框宽度（px）
     */
    fun setShapeStroke(color: Int, width: Float) {
        shapeHelper.setStroke(color, width)
    }

    /**
     * 设置圆角半径
     * @param radius 圆角半径（px）
     */
    fun setShapeRadius(radius: Float) {
        shapeHelper.setRadius(radius)
    }

    /**
     * 设置不同角的圆角半径
     * @param topLeft 左上角圆角半径
     * @param topRight 右上角圆角半径
     * @param bottomLeft 左下角圆角半径
     * @param bottomRight 右下角圆角半径
     */
    fun setShapeCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float) {
        shapeHelper.setCornerRadii(topLeft, topRight, bottomLeft, bottomRight)
    }

    /**
     * 设置渐变背景
     * @param startColor 开始颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setShapeGradientBackground(startColor: Int, endColor: Int, orientation: android.graphics.drawable.GradientDrawable.Orientation) {
        shapeHelper.setGradientBackground(startColor, endColor, orientation)
    }

    /**
     * 设置渐变背景（带中间颜色）
     * @param startColor 开始颜色
     * @param centerColor 中间颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setShapeGradientBackground(startColor: Int, centerColor: Int, endColor: Int, orientation: android.graphics.drawable.GradientDrawable.Orientation) {
        shapeHelper.setGradientBackground(startColor, centerColor, endColor, orientation)
    }
}