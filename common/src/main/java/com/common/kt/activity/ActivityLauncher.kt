package com.common.kt.activity

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 用于在非Activity类中启动Activity并接收结果的工具类
 * 使用方法:
 * 1. 创建一个ActivityLauncher实例并保存在您的类中
 * 2. 调用launch方法启动目标Activity
 * 3. 在callback中处理返回结果
 */
class ActivityLauncher(private val activity: FragmentActivity) : DefaultLifecycleObserver {

    // ActivityResultLauncher用于启动Activity并接收结果
    private var launcher: ActivityResultLauncher<Intent>? = null

    // 用于存储回调函数
    private var resultCallback: ((ActivityResult) -> Unit)? = null

    init {
        // 在构造函数中注册ActivityResultLauncher
        launcher = activity.activityResultRegistry.register(
            "customize_activity_rq#${mNextLocalRequestCode.getAndIncrement()}",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            launcher?.unregister()
            // 当Activity返回结果时，调用保存的回调函数
            resultCallback?.invoke(result)
            // 调用后清空回调，防止内存泄漏
            resultCallback = null
            launcher = null
            activity.lifecycle.removeObserver(this)
        }

        activity.lifecycle.addObserver(this)
    }

    /**
     * 启动Activity并接收结果
     * @param context 上下文
     * @param intent 要启动的Intent
     * @param callback 接收结果的回调函数
     */
    fun launch(intent: Intent, callback: (ActivityResult) -> Unit) {
        // 保存回调函数
        resultCallback = callback
        // 启动Activity
        launcher?.launch(intent)
    }

    /**
     * 当LifecycleOwner销毁时，清理资源
     */
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        launcher?.unregister()
        resultCallback = null
        launcher = null
        activity.lifecycle.removeObserver(this)
    }
}


