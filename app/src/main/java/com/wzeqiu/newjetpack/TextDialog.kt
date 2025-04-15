package com.wzeqiu.newjetpack

import android.os.Bundle
import android.view.View
import com.common.BaseDialog
import com.wzeqiu.newjetpack.databinding.LayoutTestDialogBinding

class TextDialog  :BaseDialog<LayoutTestDialogBinding>(){

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

        }
    }
}