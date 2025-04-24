package com.common.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.common.common.R
import java.lang.reflect.ParameterizedType

abstract class BaseDialog<VB : ViewBinding> : DialogFragment() {
    private var widthRatio = 1f // 默认宽度比例
    private var bgAlpha = 0.5f // 默认背景透明度
    private var gravity = Gravity.CENTER // 默认居中位置
    private var cancelableOnBackPress = true // 默认可以通过返回键关闭
    private var cancelableOnTouchOutside = true // 默认可以通过点击空白区域关闭


    /**
     * 设置对话框宽度比例 (0-1)
     */
    fun setWidthRatio(ratio: Float): BaseDialog<*> {
        widthRatio = ratio.coerceIn(0f, 1f)
        return this
    }

    /**
     * 设置背景透明度 (0-1)
     */
    fun setBackgroundAlpha(alpha: Float): BaseDialog<*> {
        bgAlpha = alpha.coerceIn(0f, 1f)
        return this
    }

    /**
     * 设置对话框位置
     */
    fun setGravity(gravity: Int): BaseDialog<*> {
        this.gravity = gravity
        return this
    }

    /**
     * 设置是否可以通过返回键关闭
     */
    fun setCancelableOnBackPress(cancelable: Boolean): BaseDialog<*> {
        this.cancelableOnBackPress = cancelable
        return this
    }

    /**
     * 设置是否可以通过点击空白区域关闭
     */
    fun setCancelableOnTouchOutside(cancelable: Boolean): BaseDialog<*> {
        this.cancelableOnTouchOutside = cancelable
        return this
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        setStyle(STYLE_NO_TITLE, R.style.BaseDialogStyle)
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCancelable(cancelableOnBackPress)
        dialog.setCanceledOnTouchOutside(cancelableOnTouchOutside)

        // 设置对话框样式
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(gravity)
            setDimAmount(bgAlpha)

        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        // 设置对话框样式
        dialog?.window?.apply {
            // 设置宽度
            attributes = attributes.apply {
                width = (resources.displayMetrics.widthPixels * widthRatio).toInt()
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
    }

    protected lateinit var binding: VB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflateBinding(inflater)
        return binding.root
    }

    @Suppress("UNCHECKED_CAST")
    private fun inflateBinding(inflater: LayoutInflater): VB {
        val bindingClass = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<VB>
        val inflateMethod = bindingClass.getMethod("inflate", LayoutInflater::class.java)
        return inflateMethod.invoke(null, inflater) as VB
    }
}