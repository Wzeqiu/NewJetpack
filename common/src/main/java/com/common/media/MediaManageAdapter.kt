package com.common.media

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chad.library.adapter4.BaseQuickAdapter
import com.common.common.databinding.ItemMediaBinding
import com.common.kt.adapter.ViewBindingHolder
import com.common.kt.load

class MediaManageAdapter(
    private val enableMultiSelect: Boolean = false
) : BaseQuickAdapter<MediaInfo, ViewBindingHolder<ItemMediaBinding>>() {

    override fun onBindViewHolder(
        holder: ViewBindingHolder<ItemMediaBinding>,
        position: Int,
        item: MediaInfo?
    ) {
        item ?: return
        
        val binding = holder.getBinding()
        // 加载媒体缩略图
        binding.ivImg.load(item.path)
        
        // 显示媒体类型指示器
        when (item.mediaType) {
            MediaConfig.MEDIA_TYPE_VIDEO -> {
                binding.ivMediaTypeIndicator.visibility = View.VISIBLE
                binding.tvDuration.visibility = View.VISIBLE
                binding.tvDuration.text = item.getFormattedDuration()
            }
            MediaConfig.MEDIA_TYPE_AUDIO -> {
                binding.ivMediaTypeIndicator.visibility = View.VISIBLE
                binding.tvDuration.visibility = View.VISIBLE
                binding.tvDuration.text = item.getFormattedDuration()
            }
            else -> {
                binding.ivMediaTypeIndicator.visibility = View.GONE
                binding.tvDuration.visibility = View.GONE
            }
        }
        
        // 处理多选UI
        if (enableMultiSelect) {
            binding.cbSelect.visibility = View.VISIBLE
            binding.cbSelect.isChecked = item.isSelect
        } else {
            binding.cbSelect.visibility = View.GONE
        }
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