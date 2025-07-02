package com.common.kt

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified VB : ViewBinding> AppCompatActivity.viewBinding() =
    ViewBindingProperty<AppCompatActivity, VB> {
        bindingInvoke<VB>(layoutInflater).also { setContentView(it.root) }
    }

inline fun <reified VB : ViewBinding> bindingInvoke(layoutInflater: LayoutInflater) =
    VB::class.java.getDeclaredMethod("inflate", LayoutInflater::class.java)(
        null, layoutInflater
    ) as VB


class ViewBindingProperty<I, O>(val initializer: (I) -> O) : ReadOnlyProperty<I, O> {
    private var _value: O? = null
    override fun getValue(thisRef: I, property: KProperty<*>): O {
        return _value ?: initializer(thisRef).also { _value = it }
    }
}


