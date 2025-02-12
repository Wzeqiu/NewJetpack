package com.common.kt.mmkv

import android.os.Parcelable
import com.tencent.mmkv.MMKV
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

fun <V> MMKV.initializer(
    default: V,
    beforeChange: (oldValue: V, newValue: V) -> Boolean = { oldValue, newValue -> oldValue == newValue }
) = MMKVKeyValue(default, this, beforeChange)

internal object UNINITIALIZED_VALUE

class MMKVKeyValue<V>(
    private val default: V?,
    private val mmkv: MMKV,
    private val beforeChange: (oldValue: V, newValue: V) -> Boolean
) : ReadWriteProperty<Any?, V> {
    private var _value: Any? = UNINITIALIZED_VALUE

    override fun getValue(thisRef: Any?, property: KProperty<*>): V {
        if (_value !== UNINITIALIZED_VALUE) return _value as V
        _value = when (property.returnType.classifier) {
            String::class -> mmkv.decodeString(property.name, default as String) as V
            Int::class -> mmkv.decodeInt(property.name, default as Int) as V
            Float::class -> mmkv.decodeFloat(property.name, default as Float) as V
            Double::class -> mmkv.decodeDouble(property.name, default as Double) as V
            Boolean::class -> mmkv.decodeBool(property.name, default as Boolean) as V
            Long::class -> mmkv.decodeLong(property.name, default as Long) as V
            else -> {
                if ((property.returnType.classifier as KClass<*>).isSubclassOf(Parcelable::class)) {
                    val classifier = property.returnType.classifier
                    mmkv.decodeParcelable(
                        property.name, (classifier as KClass<Parcelable>).java, default as? Parcelable
                    ) as V
                } else {
                    default
                }
            }
        }
        return _value as V
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        if (_value != UNINITIALIZED_VALUE && beforeChange(_value as V, value)) return
        when (property.returnType.classifier) {
            String::class -> mmkv.encode(property.name, value as String)
            Int::class -> mmkv.encode(property.name, value as Int)
            Float::class -> mmkv.encode(property.name, value as Float)
            Double::class -> mmkv.encode(property.name, value as Double)
            Boolean::class -> mmkv.encode(property.name, value as Boolean)
            Long::class -> mmkv.encode(property.name, value as Long)
            else -> {
                if ((property.returnType.classifier as KClass<*>).isSubclassOf(Parcelable::class)) {
                    mmkv.encode(property.name, value as Parcelable)
                }
            }
        }
        _value = value
    }
}





