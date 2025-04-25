package com.common.ui.webView

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.common.common.databinding.ActivityWebviewBinding
import com.common.ui.BaseActivity


class WebViewActivity : BaseActivity<ActivityWebviewBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webViewSetting()
        // 加载本地 HTML 文件或远程 URL
        binding.webView.loadUrl("file:///android_asset/index.html");
    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun webViewSetting() {
        binding.webView.settings.apply {
            // 启用 JavaScript
            javaScriptEnabled = true
            // 限制 JavaScript 权限
            // 优先使用缓存
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
            // 不使用缓存
//            cacheMode = WebSettings.LOAD_NO_CACHE;
            // 支持缩放
            setSupportZoom(false)
            builtInZoomControls = true
            displayZoomControls = false
            // 支持文件访问
            allowFileAccess = true
            // 支持 DOM 存储
            domStorageEnabled = true
            // 支持地理位置
            setGeolocationEnabled(true)
        }

        //注入对象， 注入对象名称
        binding.webView.addJavascriptInterface(WebAppInterface(), "Android")

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                request?.url?.let { url ->
                    url.scheme?.let { scheme ->
                        if (scheme.startsWith("test")) {
                            // 获取参数  param 为定义的参数字段
                            val paramValue = url.getQueryParameter("param")
                            val intent = Intent(Intent.ACTION_VIEW, request.url)
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            }
                            return true
                        }
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                // 更新 显示网页标题
            }
        }
    }


    class WebAppInterface {

        @JavascriptInterface
        fun processData(data: String): String {
            Log.e("AAAAAA", "test")
            // 模拟处理数据
            return "Processed: $data"
        }
    }
}