package com.wzeqiu.newjetpack

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.common.widget.guide.DemoGuideContentView
import com.common.widget.guide.GuidePageController

/**
 * 引导页演示活动
 */
class GuidePageDemoActivity : AppCompatActivity() {

    private lateinit var guideController: GuidePageController
    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide_page_demo)
        
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        button3 = findViewById(R.id.button3)
        
        val startGuideButton: Button = findViewById(R.id.btn_start_guide)
        startGuideButton.setOnClickListener {
            showGuide()
        }
        
        // 初始化引导控制器
        guideController = GuidePageController(this)
        
        // 设置引导结束监听器
        guideController.setOnFinishListener {
            Toast.makeText(this, "引导结束", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showGuide() {
        // 清除之前的引导步骤
        guideController.clearSteps()
        
        // 创建自定义内容视图
        val customContentView1 = createCustomContentView(
            "按钮1功能介绍"
        )
        
        val customContentView2 = createCustomContentView(
            "按钮2功能介绍"
        )
        
        val customContentView3 = createCustomContentView(
            "按钮3功能介绍",
        )
        
        // 添加引导步骤
        guideController.addSteps(
            listOf(
                // 第一步：高亮按钮1并使用自定义内容视图
                GuidePageController.GuideStep(
                    targetView = button1,
                    customContentView = customContentView1,
                    cornerRadius = 30f
                ),
                
                // 第二步：高亮按钮2并使用自定义内容视图
                GuidePageController.GuideStep(
                    targetView = button2,
                    customContentView = customContentView2,
                    cornerRadius = 30f
                ),
                
                // 第三步：高亮按钮3并使用自定义内容视图
                GuidePageController.GuideStep(
                    targetView = button3,
                    customContentView = customContentView3,
                    cornerRadius = 30f
                )
            )
        )
        
        // 显示引导
        guideController.show()
    }
    
    /**
     * 创建自定义内容视图
     */
    private fun createCustomContentView(title: String,): DemoGuideContentView {
        return DemoGuideContentView(this).apply {
            setTitle(title)
            setOnNextClickListener {
                guideController.nextStep()
            }
        }
    }
} 