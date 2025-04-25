package com.common.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val layoutManager = parent.layoutManager

        if (layoutManager is GridLayoutManager) {
            val spanCount = layoutManager.spanCount
            val column = position % spanCount

            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else if (layoutManager is LinearLayoutManager) {
            if (layoutManager.orientation == LinearLayoutManager.VERTICAL) {
                if (position == 0) {
                    outRect.top = spacing
                }
                outRect.bottom = spacing
                outRect.left = spacing
                outRect.right = spacing
            } else {
                if (position == 0) {
                    outRect.left = spacing
                }
                outRect.right = spacing
                outRect.top = spacing
                outRect.bottom = spacing
            }
        }
    }
}