package com.wzeqiu.newjetpack.view

import android.graphics.drawable.GradientDrawable

/**
 * Shape属性设置接口
 * 定义了所有支持的shape属性设置方法
 */
interface IShapeView {
    /**
     * 设置背景颜色
     * @param color 颜色值
     */
    fun setShapeBackgroundColor(color: Int)

    /**
     * 设置边框
     * @param color 边框颜色
     * @param width 边框宽度（px）
     */
    fun setShapeStroke(color: Int, width: Float)

    /**
     * 设置圆角半径
     * @param radius 圆角半径（px）
     */
    fun setShapeRadius(radius: Float)

    /**
     * 设置不同角的圆角半径
     * @param topLeft 左上角圆角半径
     * @param topRight 右上角圆角半径
     * @param bottomLeft 左下角圆角半径
     * @param bottomRight 右下角圆角半径
     */
    fun setShapeCornerRadii(topLeft: Float, topRight: Float, bottomLeft: Float, bottomRight: Float)

    /**
     * 设置渐变背景
     * @param startColor 开始颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setShapeGradientBackground(startColor: Int, endColor: Int, orientation: GradientDrawable.Orientation)

    /**
     * 设置渐变背景（带中间颜色）
     * @param startColor 开始颜色
     * @param centerColor 中间颜色
     * @param endColor 结束颜色
     * @param orientation 渐变方向
     */
    fun setShapeGradientBackground(startColor: Int, centerColor: Int, endColor: Int, orientation: GradientDrawable.Orientation)
}