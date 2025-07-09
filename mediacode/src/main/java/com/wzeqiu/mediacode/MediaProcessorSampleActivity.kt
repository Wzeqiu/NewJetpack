package com.wzeqiu.mediacode

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.common.kt.saveToAlbum
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.common.utils.media.MediaProcessor
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 媒体处理示例Activity
 * 展示如何使用协程版本的MediaProcessor
 */
class MediaProcessorSampleActivity : AppCompatActivity() {

    private val TAG = "MediaProcessorSample"
    private var selectedMediaInfo: MediaInfo? = null
    private lateinit var mediaProcessor: MediaProcessor
    private lateinit var tvStatus: TextView

    // 注册 MediaManageActivity 结果回调
    private val mediaSelectLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                // 获取单个媒体文件结果
                val mediaInfo =
                    intent.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)
                if (mediaInfo != null) {
                    handleSelectedMedia(mediaInfo)
                }

                // 获取多个媒体文件结果（如果有）
                val mediaList =
                    intent.getParcelableArrayListExtra<MediaInfo>(MediaManageActivity.RESULT_LIST_DATA)
                if (!mediaList.isNullOrEmpty()) {
                    // 在本例中我们只处理第一个选中的媒体文件
                    handleSelectedMedia(mediaList.first())
                }
            }
        }
    }

    /**
     * 处理选择的媒体文件
     */
    private fun handleSelectedMedia(mediaInfo: MediaInfo) {
        selectedMediaInfo = mediaInfo
        tvStatus.text = "已选择媒体：${mediaInfo.name}"

        // 根据媒体类型更新UI状态
        val btnExtractAudio = findViewById<Button>(R.id.btn_extract_audio)
        val btnTrimVideo = findViewById<Button>(R.id.btn_trim_video)

        // 只有视频可以提取音频和裁剪
        btnExtractAudio.isEnabled = mediaInfo.isVideo()
        btnTrimVideo.isEnabled = mediaInfo.isVideo()
        findViewById<Button>(R.id.btn_change_resolution).isEnabled = mediaInfo.isVideo()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_processor_sample)

        // 初始化UI组件
        tvStatus = findViewById(R.id.tv_status)
        val btnSelectVideo = findViewById<Button>(R.id.btn_select_video)
        val btnSelectAudio = findViewById<Button>(R.id.btn_select_audio)
        val btnExtractAudio = findViewById<Button>(R.id.btn_extract_audio)
        val btnTrimVideo = findViewById<Button>(R.id.btn_trim_video)
        val btnGetVideoInfo = findViewById<Button>(R.id.btn_get_video_info)
        val btnChangeResolution = findViewById<Button>(R.id.btn_change_resolution)


        // 初始状态下，一些按钮应该被禁用
        btnExtractAudio.isEnabled = false
        btnTrimVideo.isEnabled = false
        btnChangeResolution.isEnabled = false

        // 初始化媒体处理器
        mediaProcessor = MediaProcessor.getInstance(this)

        // 设置按钮点击事件
        btnSelectVideo.setOnClickListener {
            requestPermissionsAndSelectMedia(MediaConfig.MEDIA_TYPE_VIDEO)
        }

        btnSelectAudio.setOnClickListener {
            requestPermissionsAndSelectMedia(MediaConfig.MEDIA_TYPE_AUDIO)
        }

        btnExtractAudio.setOnClickListener {
            extractAudioFromVideo()
        }

        btnTrimVideo.setOnClickListener {
            trimVideo()
        }

        btnGetVideoInfo.setOnClickListener {
            getMediaInfo()
        }

        btnChangeResolution.setOnClickListener {
            changeVideoResolution()
        }
    }

    // 请求权限并选择媒体
    private fun requestPermissionsAndSelectMedia(mediaType: Int) {
        XXPermissions.with(this)
            .permission(Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO)
            .request { _, allGranted ->
                if (allGranted) {
                    // 创建媒体选择配置
                    val mediaConfig = MediaConfig(
                        mediaType = mediaType,      // 指定媒体类型
                        originalMedia = true,       // 使用原始文件
                        enableMultiSelect = false,  // 不启用多选
                        maxSelectCount = 1          // 最大选择数量
                    )

                    // 启动媒体选择界面
                    val intent = MediaManageActivity.getIntent(this, mediaConfig)
                    mediaSelectLauncher.launch(intent)
                } else {
                    Toast.makeText(this, "需要存储权限以选择媒体文件", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // 提取视频中的音频
    private fun extractAudioFromVideo() {
        val mediaInfo = selectedMediaInfo ?: run {
            Toast.makeText(this, "请先选择视频", Toast.LENGTH_SHORT).show()
            return
        }

        if (!mediaInfo.isVideo()) {
            Toast.makeText(this, "请选择视频文件", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            tvStatus.text = "正在提取音频..."

            runCatching {
                // 创建输出文件路径
                val fileName = "extracted_audio_${System.currentTimeMillis()}.mp3"
                val outputFile = File(cacheDir, fileName)

                // 执行音频提取
                withContext(Dispatchers.IO) {
                    mediaProcessor.extractAudio(mediaInfo.path, outputFile.absolutePath)
                }

                tvStatus.text = "音频提取成功：${outputFile.name}"
                Toast.makeText(
                    this@MediaProcessorSampleActivity,
                    "音频提取成功",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                Log.e(TAG, "提取音频失败", e)
                tvStatus.text = "提取音频失败: ${e.message}"
                Toast.makeText(
                    this@MediaProcessorSampleActivity,
                    "提取音频失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 裁剪视频
    private fun trimVideo() {
        val mediaInfo = selectedMediaInfo ?: run {
            Toast.makeText(this, "请先选择视频", Toast.LENGTH_SHORT).show()
            return
        }

        if (!mediaInfo.isVideo()) {
            Toast.makeText(this, "请选择视频文件", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            tvStatus.text = "正在裁剪视频..."

            runCatching {
                // 获取视频时长
                val duration = withContext(Dispatchers.IO) {
                    mediaProcessor.getMediaDuration(mediaInfo.path)
                }

                // 裁剪视频的前10秒（如果视频不足10秒则裁剪全部）
                val trimDuration = if (duration > 10000) 10000 else duration

                // 创建输出文件路径
                val fileName = "trimmed_video_${System.currentTimeMillis()}.mp4"
                val outputFile = File(cacheDir, fileName)

                // 执行视频裁剪
                withContext(Dispatchers.IO) {
                    mediaProcessor.trimMedia(
                        mediaInfo.path,
                        outputFile.absolutePath,
                        0, // 从开始裁剪
                        trimDuration // 裁剪时长（毫秒）
                    )
                }

                this@MediaProcessorSampleActivity.saveToAlbum(mutableListOf(outputFile.absolutePath))
                tvStatus.text =
                    "视频裁剪成功：${outputFile.name}，时长: ${formatDuration(trimDuration)}"
                Toast.makeText(
                    this@MediaProcessorSampleActivity,
                    "视频裁剪成功",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                Log.e(TAG, "裁剪视频失败", e)
                tvStatus.text = "裁剪视频失败: ${e.message}"
                Toast.makeText(
                    this@MediaProcessorSampleActivity,
                    "裁剪视频失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun changeVideoResolution() {
        val mediaInfo = selectedMediaInfo ?: run {
            Toast.makeText(this, "请先选择视频", Toast.LENGTH_SHORT).show()
            return
        }

        if (!mediaInfo.isVideo()) {
            Toast.makeText(this, "请选择视频文件", Toast.LENGTH_SHORT).show()
            return
        }

        val width = findViewById<EditText>(R.id.et_width).text.toString().toIntOrNull()
        val height = findViewById<EditText>(R.id.et_height).text.toString().toIntOrNull()

        if (width == null || height == null || width <= 0 || height <= 0) {
            Toast.makeText(this, "请输入有效的宽高", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            tvStatus.text = "正在修改分辨率..."
            runCatching {
                val fileName = "resolution_changed_${System.currentTimeMillis()}.mp4"
                val outputFile = File(cacheDir, fileName)

                withContext(Dispatchers.IO) {
                    mediaProcessor.changeVideoResolution(
                        mediaInfo.path,
                        outputFile.absolutePath,
                        width,
                        height
                    )
                }
                this@MediaProcessorSampleActivity.saveToAlbum(mutableListOf(outputFile.absolutePath))
                tvStatus.text = "分辨率修改成功: ${outputFile.name}"
                Toast.makeText(this@MediaProcessorSampleActivity, "分辨率修改成功", Toast.LENGTH_SHORT)
                    .show()
            }.onFailure { e ->
                Log.e(TAG, "修改分辨率失败", e)
                tvStatus.text = "修改分辨率失败: ${e.message}"
                Toast.makeText(
                    this@MediaProcessorSampleActivity,
                    "修改分辨率失败",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 获取媒体信息
    private fun getMediaInfo() {
        val mediaInfo = selectedMediaInfo ?: run {
            Toast.makeText(this, "请先选择媒体文件", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            runCatching {
                // 获取媒体信息
                val updatedMediaInfo = withContext(Dispatchers.IO) {
                    mediaProcessor.getMediaInfo(mediaInfo.path)
                }

                if (updatedMediaInfo != null) {
                    val infoText = buildString {
                        append("媒体信息:\n")
                        append("名称: ${updatedMediaInfo.name}\n")
                        append("路径: ${updatedMediaInfo.path}\n")
                        append("大小: ${updatedMediaInfo.getFormattedSize()}\n")
                        append(
                            "类型: ${
                                when (updatedMediaInfo.mediaType) {
                                    MediaConfig.MEDIA_TYPE_IMAGE -> "图片"
                                    MediaConfig.MEDIA_TYPE_VIDEO -> "视频"
                                    MediaConfig.MEDIA_TYPE_AUDIO -> "音频"
                                    else -> "未知"
                                }
                            }\n"
                        )

                        if (updatedMediaInfo.isVideo() || updatedMediaInfo.isAudio()) {
                            append("时长: ${updatedMediaInfo.getFormattedDuration()}\n")
                        }

                        if (updatedMediaInfo.isVideo() || updatedMediaInfo.isImage()) {
                            append("分辨率: ${updatedMediaInfo.width} x ${updatedMediaInfo.height}\n")
                        }

                        append("MD5: ")
                    }

                    tvStatus.text = infoText + "计算中..."

                    // 计算文件MD5
                    val md5 = withContext(Dispatchers.IO) {
                        mediaProcessor.calculateMD5(mediaInfo.path)
                    }

                    tvStatus.text = infoText + md5
                } else {
                    tvStatus.text = "无法获取媒体信息"
                }
            }.onFailure { e ->
                Log.e(TAG, "获取媒体信息失败", e)
                tvStatus.text = "获取媒体信息失败: ${e.message}"
            }
        }
    }

    // 辅助方法：格式化时长
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
} 