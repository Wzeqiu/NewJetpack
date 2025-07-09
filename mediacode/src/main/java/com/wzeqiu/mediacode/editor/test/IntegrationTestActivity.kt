package com.wzeqiu.mediacode.editor.test

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.common.ui.BaseActivity
import com.wzeqiu.mediacode.R
import com.wzeqiu.mediacode.databinding.ActivityIntegrationTestBinding
import com.wzeqiu.mediacode.editor.VideoEditorViewModel

/**
 * 集成测试Activity
 * 
 * 用于测试视频编辑器所有功能的协同工作
 */
class IntegrationTestActivity : BaseActivity<ActivityIntegrationTestBinding>() {
    
    companion object {
        private const val TAG = "IntegrationTest"
    }
    
    // ViewModel
    private lateinit var viewModel: VideoEditorViewModel
    
    // 视图组件
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var buttonStartTest: Button
    private lateinit var buttonCancelTest: Button
    
    // 测试助手
    private lateinit var testHelper: VideoEditorTestHelper
    
    // 测试是否正在运行
    private var isTestRunning = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_integration_test)
        
        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[VideoEditorViewModel::class.java]
        
        // 初始化测试助手
        testHelper = VideoEditorTestHelper(this)
        
        // 初始化视图组件
        initViews()
        
        // 设置点击事件
        setupClickListeners()
        
        // 观察导出状态
        observeExportStatus()
    }
    
    /**
     * 初始化视图组件
     */
    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        buttonStartTest = findViewById(R.id.buttonStartTest)
        buttonCancelTest = findViewById(R.id.buttonCancelTest)
        
        // 初始状态
        progressBar.visibility = View.GONE
        buttonCancelTest.visibility = View.GONE
    }
    
    /**
     * 设置点击事件
     */
    private fun setupClickListeners() {
        // 开始测试按钮
        buttonStartTest.setOnClickListener {
            if (!isTestRunning) {
                startTest()
            }
        }
        
        // 取消测试按钮
        buttonCancelTest.setOnClickListener {
            if (isTestRunning) {
                cancelTest()
            }
        }
    }
    
    /**
     * 观察导出状态
     */
    private fun observeExportStatus() {
        viewModel.exportStatus.observe(this) { status ->
            when (status) {
                is VideoEditorViewModel.ExportStatus.Preparing -> {
                    updateStatus("准备导出...")
                }
                is VideoEditorViewModel.ExportStatus.Processing -> {
                    updateStatus("导出中: ${status.progress}%")
                    progressBar.progress = status.progress
                }
                is VideoEditorViewModel.ExportStatus.Completed -> {
                    updateStatus("导出完成: ${status.outputPath}")
                    testCompleted(true)
                }
                is VideoEditorViewModel.ExportStatus.Error -> {
                    updateStatus("导出失败: ${status.message}")
                    testCompleted(false)
                }
            }
        }
    }
    
    /**
     * 开始测试
     */
    private fun startTest() {
        Log.d(TAG, "开始集成测试")
        
        // 更新UI状态
        isTestRunning = true
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        buttonStartTest.visibility = View.GONE
        buttonCancelTest.visibility = View.VISIBLE
        updateStatus("正在初始化测试...")
        
        // 执行测试
        testHelper.runFullTest(viewModel) { success ->
            // 在主线程更新UI
            runOnUiThread {
                if (success) {
                    updateStatus("测试完成！所有功能测试通过。")
                } else {
                    updateStatus("测试失败，请查看日志了解详情。")
                }
                testCompleted(success)
            }
        }
    }
    
    /**
     * 取消测试
     */
    private fun cancelTest() {
        Log.d(TAG, "取消集成测试")
        
        // 更新UI状态
        testCompleted(false)
        updateStatus("测试已取消")
    }
    
    /**
     * 测试完成
     */
    private fun testCompleted(success: Boolean) {
        // 更新UI状态
        isTestRunning = false
        buttonStartTest.visibility = View.VISIBLE
        buttonCancelTest.visibility = View.GONE
        
        // 显示结果
        val message = if (success) {
            "测试完成"
        } else {
            "测试未完成"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 更新状态文本
     */
    private fun updateStatus(status: String) {
        statusText.text = status
        Log.d(TAG, status)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 释放资源
        viewModel.releaseResources()
    }
} 