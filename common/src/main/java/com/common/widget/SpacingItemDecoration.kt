package com.common.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * RecyclerView的间距装饰器，支持多种布局管理器和自定义边距
 * @param spacingLeft 左边距
 * @param spacingTop 上边距
 * @param spacingRight 右边距
 * @param spacingBottom 下边距
 * @param includeEdge 是否包含边缘间距
 */
class SpacingItemDecoration @JvmOverloads constructor(
    private val spacingLeft: Int = 0,
    private val spacingTop: Int = 0,
    private val spacingRight: Int = 0,
    private val spacingBottom: Int = 0,
    private val includeEdge: Boolean = false
) : RecyclerView.ItemDecoration() {

    /**
     * 使用相同的间距值创建装饰器
     * @param spacing 所有方向的间距值
     * @param includeEdge 是否包含边缘间距
     */
    constructor(spacing: Int, includeEdge: Boolean = false) : this(
        spacing, spacing, spacing, spacing, includeEdge
    )

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position < 0) return // 无效位置不处理

        when (val layoutManager = parent.layoutManager) {
            is GridLayoutManager -> applyGridOffsets(
                outRect,
                position,
                layoutManager.spanCount,
                layoutManager.orientation
            )

            is StaggeredGridLayoutManager -> applyStaggeredGridOffsets(
                outRect,
                position,
                view,
                layoutManager
            )

            is LinearLayoutManager -> applyLinearOffsets(
                outRect,
                position,
                layoutManager.orientation
            )
        }
    }

    private fun applyGridOffsets(outRect: Rect, position: Int, spanCount: Int, orientation: Int) {
        // 计算当前项在网格中的行和列
        val column = position % spanCount
        val row = position / spanCount

        if (orientation == RecyclerView.VERTICAL) {
            if (includeEdge) {
                // 左边距 - 平均分配列间距
                outRect.left = spacingLeft - column * spacingLeft / spanCount
                // 右边距 - 平均分配列间距
                outRect.right = (column + 1) * spacingRight / spanCount

                // 第一行需要上边距
                if (row == 0) {
                    outRect.top = spacingTop
                }
                outRect.bottom = spacingBottom
            } else {
                // 不包含边缘时的计算
                outRect.left = column * spacingLeft / spanCount
                outRect.right = spacingRight - (column + 1) * spacingRight / spanCount

                if (row > 0) {
                    outRect.top = spacingTop
                }
            }
        } else {
            if (includeEdge) {
                // 上边距 - 平均分配行间距
                outRect.top = spacingTop - row * spacingTop / spanCount
                // 下边距 - 平均分配行间距
                outRect.bottom = (row + 1) * spacingBottom / spanCount

                // 第一列需要左边距
                if (column == 0) {
                    outRect.left = spacingLeft
                }
                outRect.right = spacingRight
            } else {
                // 不包含边缘时的计算
                outRect.top = row * spacingTop / spanCount
                outRect.bottom = spacingBottom - (row + 1) * spacingBottom / spanCount

                if (column > 0) {
                    outRect.left = spacingLeft
                }
            }
        }
    }

    private fun applyStaggeredGridOffsets(
        outRect: Rect,
        position: Int,
        view: View,
        layoutManager: StaggeredGridLayoutManager
    ) {
        val params = view.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        val spanIndex = params?.spanIndex ?: 0
        val spanCount = layoutManager.spanCount

        if (layoutManager.orientation == StaggeredGridLayoutManager.VERTICAL) {
            if (includeEdge) {
                outRect.left = spacingLeft - spanIndex * spacingLeft / spanCount
                outRect.right = (spanIndex + 1) * spacingRight / spanCount

                // 对于第一行的处理需要额外逻辑，这里简化处理
                if (position < spanCount) {
                    outRect.top = spacingTop
                }
                outRect.bottom = spacingBottom
            } else {
                outRect.left = spanIndex * spacingLeft / spanCount
                outRect.right = spacingRight - (spanIndex + 1) * spacingRight / spanCount
                outRect.bottom = spacingBottom
            }
        } else {
            if (includeEdge) {
                outRect.top = spacingTop - spanIndex * spacingTop / spanCount
                outRect.bottom = (spanIndex + 1) * spacingBottom / spanCount

                // 对于第一列的处理
                if (position < spanCount) {
                    outRect.left = spacingLeft
                }
                outRect.right = spacingRight
            } else {
                outRect.top = spanIndex * spacingTop / spanCount
                outRect.bottom = spacingBottom - (spanIndex + 1) * spacingBottom / spanCount
                outRect.right = spacingRight
            }
        }
    }

    private fun applyLinearOffsets(outRect: Rect, position: Int, orientation: Int) {
        if (orientation == RecyclerView.VERTICAL) {
            outRect.left = spacingLeft
            outRect.right = spacingRight

            if (includeEdge) {
                // 包含边缘时，第一项需要上边距
                if (position == 0) {
                    outRect.top = spacingTop
                }
                outRect.bottom = spacingBottom
            } else {
                // 不包含边缘时，除第一项外都有上边距
                if (position > 0) {
                    outRect.top = spacingTop
                }
            }
        } else {
            outRect.top = spacingTop
            outRect.bottom = spacingBottom

            if (includeEdge) {
                // 包含边缘时，第一项需要左边距
                if (position == 0) {
                    outRect.left = spacingLeft
                }
                outRect.right = spacingRight
            } else {
                // 不包含边缘时，除第一项外都有左边距
                if (position > 0) {
                    outRect.left = spacingLeft
                }
            }
        }
    }
}