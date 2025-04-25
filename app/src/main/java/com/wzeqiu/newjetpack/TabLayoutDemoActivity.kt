package com.wzeqiu.newjetpack

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import com.common.ui.BaseActivity
import com.common.widget.tab.setupWithViewPager
import com.google.android.material.tabs.TabLayout
import com.wzeqiu.newjetpack.databinding.ActivityTabLayoutDemoBinding

class TabLayoutDemoActivity : BaseActivity<ActivityTabLayoutDemoBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.apply {
            tabLayout.tabMode = TabLayout.MODE_SCROLLABLE
            tabLayout.setupWithViewPager(
                this@TabLayoutDemoActivity,
                viewPage2,
                mutableListOf("测试1", "测试2", "测试3", "测试4", "测试5", "测试6"), {
                    val textView = TextView(this@TabLayoutDemoActivity)
                    textView.text = "测试$it"
                    textView.gravity = Gravity.CENTER
                    textView
                }) {
                return@setupWithViewPager TabLayoutDemoFragment()

            }

        }


    }
}