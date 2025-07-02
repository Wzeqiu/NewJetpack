package com.common.kt

import android.annotation.SuppressLint
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners


fun ImageView.load(url: Any) {
    Glide.with(this).load(url).into(this)
}

fun ImageView.loadRound(path: Any, radius: Int = 4f.dp2px) {
    Glide.with(this)
        .load(path)
        .transform(CenterCrop(), RoundedCorners(radius)).into(this)
}

/**
 * 使用 Glide 加载图片并进行等比缩放适配 ImageView 的指定尺寸
 * @param url 图片的 URL 或路径
 * @param targetWidth 目标宽度，如果已知宽度则传入，未知则为 0
 * @param targetHeight 目标高度，如果已知高度则传入，未知则为 0
 */
@SuppressLint("CheckResult")
fun ImageView.loadScaled(url: Any, targetWidth: Int = 0, targetHeight: Int = 0) {
    val requestBuilder = Glide.with(this).load(url)

    if (targetWidth > 0 && targetHeight > 0) {
        // 如果同时指定了宽高，使用 override 精确控制尺寸
        requestBuilder.override(targetWidth, targetHeight)
    } else if (targetWidth > 0) {
        // 如果只指定了宽度，使用 override 控制宽度，高度自动计算以保持比例
        requestBuilder.override(targetWidth, com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
    } else if (targetHeight > 0) {
        // 如果只指定了高度，使用 override 控制高度，宽度自动计算以保持比例
        requestBuilder.override(
            com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
            targetHeight
        )
    }

    // 使用 FitCenter 或 CenterInside 进行等比缩放适配
    // FitCenter: 等比缩放，使图片完全显示在 ImageView 内，可能会留白
    // CenterInside: 等比缩放，使图片完全显示在 ImageView 内，可能会留白 (与 FitCenter 类似，但通常用于大图缩小)
    // CenterCrop: 等比缩放，使图片填满 ImageView，可能会裁剪
    // 这里选择 FitCenter 或 CenterInside 更符合"等比缩放适配"的要求，具体取决于希望留白还是裁剪
    // 考虑到用户需求是"等比缩放展示"，FitCenter 或 CenterInside 更合适。这里使用 FitCenter。
    requestBuilder.transform(com.bumptech.glide.load.resource.bitmap.FitCenter())
        .into(this)
}

/**
 * 整合圆角和尺寸设置的图片加载函数
 * @param url 图片的 URL 或路径
 * @param radius 圆角半径，默认为4dp
 * @param targetWidth 目标宽度，如果已知宽度则传入，未知则为 0
 * @param targetHeight 目标高度，如果已知高度则传入，未知则为 0
 * @param centerCrop 是否使用CenterCrop模式，默认为true
 */
@SuppressLint("CheckResult")
fun ImageView.loadScaledRound(
    url: Any,
    radius: Int = 4f.dp2px,
    targetWidth: Int = 0,
    targetHeight: Int = 0,
) {
    val requestBuilder = Glide.with(this).load(url)

    // 设置宽高
    if (targetWidth > 0 && targetHeight > 0) {
        requestBuilder.override(targetWidth, targetHeight)
    } else if (targetWidth > 0) {
        requestBuilder.override(targetWidth, com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
    } else if (targetHeight > 0) {
        requestBuilder.override(
            com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
            targetHeight
        )
    }

    // 不裁剪情况下的圆角处理
    requestBuilder.transform(
        com.bumptech.glide.load.resource.bitmap.FitCenter(),
        RoundedCorners(radius)
    )

    requestBuilder.into(this)
}