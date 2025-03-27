package com.common.kt.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

class ViewBindingHolder<DB : ViewBinding>(private val viewBinding: ViewBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {
    fun getBinding() = viewBinding as DB

}