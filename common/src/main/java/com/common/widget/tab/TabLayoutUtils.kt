package com.common.widget.tab

import android.graphics.Color
import android.view.View
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * 封装 TabLayout 和 ViewPager2 的联动设置
 *
 * @param activity FragmentActivity 上下文
 * @param viewPager ViewPager2 实例
 * @param fragments Fragment 列表
 * @param titles Tab 标题列表 (可选, 如果为 null 或大小与 fragments 不匹配，则不显示标题)
 */
fun <T> TabLayout.setupWithViewPager(
    activity: FragmentActivity,
    viewPager: ViewPager2,
    titles: List<T> = mutableListOf(),
    customView: T.(Int) -> View? = { _ -> null },
    selectTabAction: View.(T, Int) -> Unit = { _, _ -> },
    unselectedAction: View.(T, Int) -> Unit = { _, _ -> },
    fragmentInvoke: T.(Int) -> Fragment
) {

    viewPager.adapter = object : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = titles.size
        override fun createFragment(position: Int): Fragment =
            fragmentInvoke.invoke(titles[position], position)
    }
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(p0: TabLayout.Tab?) {
            p0?.customView?.let {
                selectTabAction(titles[p0.position], p0.position)
            }
        }

        override fun onTabUnselected(p0: TabLayout.Tab?) {
            p0?.customView?.let {
                unselectedAction(titles[p0.position], p0.position)
            }
        }

        override fun onTabReselected(p0: TabLayout.Tab?) {
        }
    })

    TabLayoutMediator(this, viewPager) { tab, position ->
        customView(titles[position], position)?.let {
            tab.view.setPadding(0)
            tab.view.setBackgroundColor(Color.parseColor("#00000000"))
            tab.customView = it
        }
        // 可以根据需要设置其他 Tab 样式
    }.attach()
}


/**
 * 封装 TabLayout 和 ViewPager2 的联动设置 (使用 FragmentManager)
 *
 * @param fragment Fragment 实例
 * @param viewPager ViewPager2 实例
 * @param fragments Fragment 列表
 * @param titles Tab 标题列表 (可选, 如果为 null 或大小与 fragments 不匹配，则不显示标题)
 */
fun <T> TabLayout.setupWithViewPager(
    fragment: Fragment,
    viewPager: ViewPager2,
    titles: List<T> = mutableListOf(),
    customView: T.(Int) -> View? = { _ -> null },
    selectTabAction: View.(T, Int) -> Unit = { _, _ -> },
    unselectedAction: View.(T, Int) -> Unit = { _, _ -> },
    fragmentInvoke: T.(Int) -> Fragment
) {

    viewPager.adapter = object : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = titles.size
        override fun createFragment(position: Int): Fragment =
            fragmentInvoke.invoke(titles[position], position)
    }
    addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(p0: TabLayout.Tab?) {
            p0?.customView?.let {
                selectTabAction(titles[p0.position], p0.position)
            }
        }

        override fun onTabUnselected(p0: TabLayout.Tab?) {
            p0?.customView?.let {
                unselectedAction(titles[p0.position], p0.position)
            }
        }

        override fun onTabReselected(p0: TabLayout.Tab?) {
        }
    })

    TabLayoutMediator(this, viewPager) { tab, position ->
        customView(titles[position], position)?.let {
            tab.view.setPadding(0)
//            tab.view.updateLayoutParams<MarginLayoutParams> {
//                marginStart=0
//                marginEnd=0
//            }
            tab.view.setBackgroundColor(Color.parseColor("#00000000"))
            tab.customView = it
        }
        // 可以根据需要设置其他 Tab 样式
    }.attach()
}