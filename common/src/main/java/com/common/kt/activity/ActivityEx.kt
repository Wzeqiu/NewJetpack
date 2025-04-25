package com.common.kt.activity

import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.hjq.permissions.XXPermissions
import java.util.concurrent.atomic.AtomicInteger


/**
 * activity 通用跳转
 * @param pairs 参数
 * @param resultCallback 回调 不传普通跳转，定义回调为传值回调
 */
inline fun <reified AT> LifecycleOwner.launch(
    vararg pairs: Pair<String, Any?>,
    noinline resultCallback: ((ActivityResult) -> Unit)? = null
) {
    val intent = toIntent<AT>(*pairs)
    resultCallback?.let {
        getActivity().launch(intent, resultCallback)
    } ?: run {
        launch(intent)
    }
}


/**
 * 启动activity
 */
fun LifecycleOwner.launch(intent: Intent) {
    getActivity().startActivity(intent)
}

/**
 * 获取intent
 */
inline fun <reified AT> LifecycleOwner.toIntent(vararg pairs: Pair<String, Any?>): Intent {
    return Intent(getActivity(), AT::class.java).putExtras(bundleOf(*pairs))
}


/**
 * 自动计数防止code 冲突
 */
val mNextLocalRequestCode: AtomicInteger by lazy { AtomicInteger() }

fun FragmentActivity.launch(
    intent: Intent, resultCallback: ((ActivityResult) -> Unit)? = null
) {
    var launcher: ActivityResultLauncher<Intent>? = null
    activityResultRegistry.register(
        "customize_activity_rq#${mNextLocalRequestCode.getAndIncrement()}",
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        launcher?.unregister()
        resultCallback?.invoke(result)
    }.apply {
        launcher = this
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            lifecycle.removeObserver(this)
            launcher?.unregister()
        }
    })
    launcher?.launch(intent)
}


/**
 * 获取activity
 */
fun LifecycleOwner.getActivity(): FragmentActivity {
    return when (this) {
        is AppCompatActivity -> this
        is Fragment -> this.requireActivity()
        else -> FragmentActivity()
    }
}





fun AppCompatActivity.requestPermission(
    vararg permission: String = arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO),
    action: () -> Unit = {}
) {
    XXPermissions.with(this).permission(permission)
        .request { p0, p1 -> if (p1) action() }
}

fun Fragment.requestPermission(
    vararg permission: String = arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VIDEO),
    action: () -> Unit = {}
) {
    XXPermissions.with(this).permission(permission)
        .request { p0, p1 -> if (p1) action() }
}
