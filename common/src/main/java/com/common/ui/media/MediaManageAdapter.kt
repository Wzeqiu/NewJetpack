package com.common.ui.media

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.chad.library.adapter4.BaseQuickAdapter
import com.common.common.databinding.ItemMediaBinding
import com.common.kt.adapter.ViewBindingHolder
import com.common.kt.load

class MediaManageAdapter : BaseQuickAdapter<MediaInfo, ViewBindingHolder<ItemMediaBinding>>() {

    override fun onBindViewHolder(
        holder: ViewBindingHolder<ItemMediaBinding>,
        position: Int,
        item: MediaInfo?
    ) {
        item ?: return
        holder.getBinding().ivImg.load(item.path)
    }

    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): ViewBindingHolder<ItemMediaBinding> {
        return ViewBindingHolder(
            ItemMediaBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }
}