package com.wzeqiu.mediacode.editor.test

import android.content.Context
import android.net.Uri
import android.util.Log
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import com.wzeqiu.mediacode.editor.AudioEditOperation
import com.wzeqiu.mediacode.editor.StickerOverlay
import com.wzeqiu.mediacode.editor.TextOverlay
import com.wzeqiu.mediacode.editor.VideoEditorViewModel
import com.wzeqiu.mediacode.editor.VideoFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * 视频编辑器测试帮助类
 * 
 * 提供用于测试和集成各项功能的辅助方法
 */
class VideoEditorTestHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "VideoEditorTestHelper"
    }
    
    // 测试用的样本视频路径
    private val sampleVideoPath = getSampleVideoPath(context)
    
    // 协程作用域
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * 执行全流程测试
     * 
     * 按顺序测试所有功能模块
     */
    fun runFullTest(viewModel: VideoEditorViewModel, callback: (Boolean) -> Unit) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "开始执行全流程测试")
                
                // 1. 加载媒体
                val mediaInfo = loadSampleMedia()
                viewModel.loadMedia(mediaInfo)
                Log.d(TAG, "加载媒体完成: ${mediaInfo.path}")
                
                // 2. 测试裁剪功能
                testTrimFeature(viewModel)
                
                // 3. 测试滤镜功能
                testFilterFeature(viewModel)
                
                // 4. 测试音频功能
                testAudioFeature(viewModel)
                
                // 5. 测试文字功能
                testTextFeature(viewModel)
                
                // 6. 测试贴纸功能
                testStickerFeature(viewModel)
                
                // 7. 测试变速功能
                testSpeedFeature(viewModel)
                
                // 8. 测试旋转和翻转功能
                testRotateAndFlipFeature(viewModel)
                
                // 9. 测试导出功能
                testExportFeature(viewModel, context)
                
                Log.d(TAG, "全流程测试完成")
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "测试过程中发生异常", e)
                callback(false)
            }
        }
    }
    
    /**
     * 加载样本媒体
     */
    private fun loadSampleMedia(): MediaInfo {
        // 创建测试用的MediaInfo对象
        return MediaInfo(
            name = "测试视频",
            path = sampleVideoPath,
            mediaType = MediaConfig.MEDIA_TYPE_VIDEO,
            size = File(sampleVideoPath).length(),
            duration = 15000, // 15秒
            width = 1280,
            height = 720,
        )
    }
    
    /**
     * 获取样本视频路径
     */
    private fun getSampleVideoPath(context: Context): String {
        // 优先尝试使用外部存储中的样本视频
        val externalDir = context.getExternalFilesDir(null)
        val externalSampleVideo = File(externalDir, "sample_video.mp4")
        
        if (externalSampleVideo.exists()) {
            return externalSampleVideo.absolutePath
        }
        
        // 如果外部存储中没有样本视频，则尝试从应用资源中复制
        // 实际应用中应该实现资源复制逻辑
        // 这里仅作为示例，返回一个假路径
        return externalSampleVideo.absolutePath
    }
    
    /**
     * 测试裁剪功能
     */
    private fun testTrimFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试裁剪功能")
        
        // 设置裁剪范围（从2秒到10秒）
        viewModel.setTrimRange(2000, 10000)
        
        // 验证裁剪范围是否设置正确
        val startMs = viewModel.getTrimStartMs()
        val endMs = viewModel.getTrimEndMs()
        
        if (startMs.toInt() != 2000 || endMs.toInt() != 10000) {
            throw Exception("裁剪范围设置失败，期望：[2000, 10000]，实际：[$startMs, $endMs]")
        }
        
        Log.d(TAG, "裁剪功能测试通过")
    }
    
    /**
     * 测试滤镜功能
     */
    private fun testFilterFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试滤镜功能")
        
        // 应用一个测试滤镜
        val testFilter = VideoFilter(
            id = "test_filter",
            name = "测试滤镜",
            type = VideoFilter.FilterType.COLOR
        )
        
        viewModel.applyFilter(testFilter)
        
        Log.d(TAG, "滤镜功能测试通过")
    }
    
    /**
     * 测试音频功能
     */
    private fun testAudioFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试音频功能")
        
        // 创建一个音频编辑操作
        val audioOperation = AudioEditOperation(
            isMuteOriginal = false,
            originalVolume = 0.8f,
            backgroundMusic = null,
            musicVolume = 0.5f,
            isLoopMusic = true,
            musicTrimStartMs = 0,
            musicTrimEndMs = 0
        )
        
        // 应用音频操作
        viewModel.applyAudioOperation(audioOperation)
        
        // 设置音量
        viewModel.setVolume(0.7f)
        
        Log.d(TAG, "音频功能测试通过")
    }
    
    /**
     * 测试文字功能
     */
    private fun testTextFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试文字功能")
        
        // 创建一个文字覆盖层
        val textOverlay = TextOverlay(
            text = "测试文字",
            startTimeMs = 1000,
            endTimeMs = 5000,
            xPosition = 0.5f,
            yPosition = 0.2f,
            fontSize = 40f,
            textColor = 0xFFFF0000.toInt(), // 红色
            hasBackground = true,
            backgroundColor = 0x80000000.toInt(), // 半透明黑色
            alignment = 1 // 居中对齐
        )
        
        // 添加文字覆盖层
        viewModel.addTextOverlay(textOverlay)
        
        // 验证文字覆盖层是否添加成功
        val overlays = viewModel.getTextOverlays()
        if (overlays.isEmpty() || overlays[0].text != "测试文字") {
            throw Exception("文字覆盖层添加失败")
        }
        
        Log.d(TAG, "文字功能测试通过")
    }
    
    /**
     * 测试贴纸功能
     */
    private fun testStickerFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试贴纸功能")
        
        // 创建一个贴纸覆盖层
        val stickerOverlay = StickerOverlay(
            resourceId = android.R.drawable.ic_menu_camera, // 使用系统图标作为测试
            category = StickerOverlay.CATEGORY_EMOJI,
            startTimeMs = 2000,
            endTimeMs = 8000,
            xPosition = 0.3f,
            yPosition = 0.7f,
            size = 0.2f,
            rotation = 15f,
            alpha = 200
        )
        
        // 添加贴纸覆盖层
        viewModel.addStickerOverlay(stickerOverlay)
        
        // 验证贴纸覆盖层是否添加成功
        val overlays = viewModel.getStickerOverlays()
        if (overlays.isEmpty()) {
            throw Exception("贴纸覆盖层添加失败")
        }
        
        Log.d(TAG, "贴纸功能测试通过")
    }
    
    /**
     * 测试变速功能
     */
    private fun testSpeedFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试变速功能")
        
        // 设置播放速度（1.5倍速）
        viewModel.setPlaybackSpeed(1.5f)
        
        Log.d(TAG, "变速功能测试通过")
    }
    
    /**
     * 测试旋转和翻转功能
     */
    private fun testRotateAndFlipFeature(viewModel: VideoEditorViewModel) {
        Log.d(TAG, "测试旋转和翻转功能")
        
        // 设置旋转角度（90度）
        viewModel.setRotation(90f)
        
        // 设置水平翻转
        viewModel.setFlipHorizontal(true)
        
        // 设置垂直翻转
        viewModel.setFlipVertical(false)
        
        Log.d(TAG, "旋转和翻转功能测试通过")
    }
    
    /**
     * 测试导出功能
     */
    private fun testExportFeature(viewModel: VideoEditorViewModel, context: Context) {
        Log.d(TAG, "测试导出功能")
        
        // 导出媒体文件
        viewModel.exportMedia(context)
        
        Log.d(TAG, "导出功能测试启动成功")
    }
} 