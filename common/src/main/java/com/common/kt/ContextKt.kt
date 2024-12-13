package com.common.kt

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.hjq.permissions.XXPermissions

fun AppCompatActivity.requestPermission(vararg permission: String, action: () -> Unit = {}) {
    XXPermissions.with(this).permission(permission)
        .request { p0, p1 -> if (p1) action() }
}

fun android.app.Fragment.requestPermission(vararg permission: String, action: () -> Unit = {}) {
    XXPermissions.with(this).permission(permission)
        .request { p0, p1 -> if (p1) action() }
}


inline fun <reified AT> AppCompatActivity.toActivity(vararg pairs: Pair<String, Any?>) {
    startActivity(toIntent<AT>(*pairs))
}

inline fun <reified AT> Fragment.toActivity(vararg pairs: Pair<String, Any?>) {
    startActivity(toIntent<AT>(*pairs))
}

inline fun <reified AT> AppCompatActivity.toIntent(vararg pairs: Pair<String, Any?>): Intent {
    return Intent(this, AT::class.java).putExtras(bundleOf(*pairs))
}

inline fun <reified AT> Fragment.toIntent(vararg pairs: Pair<String, Any?>): Intent {
    return Intent(requireActivity(), AT::class.java).putExtras(bundleOf(*pairs))
}


