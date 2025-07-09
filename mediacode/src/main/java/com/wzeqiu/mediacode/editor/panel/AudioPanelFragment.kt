package com.wzeqiu.mediacode.editor.panel

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.common.kt.formatDuration
import com.common.kt.toastLong
import com.common.kt.toastShort
import com.common.media.MediaConfig
import com.common.media.MediaInfo
import com.common.media.MediaManageActivity
import com.wzeqiu.mediacode.databinding.PanelAudioBinding
import com.wzeqiu.mediacode.editor.AudioEditOperation
import com.wzeqiu.mediacode.editor.VideoEditorViewModel
import java.io.File
import java.util.Locale

/**
 * 音频编辑面板Fragment
 *
 * 提供音频相关的编辑功能，包括：
 * - 原始音频音量控制
 * - 背景音乐添加和控制
 * - 音频提取
 * - 渐入渐出效果
 */
class AudioPanelFragment : Fragment() {

    private lateinit var binding: PanelAudioBinding
    private lateinit var viewModel: VideoEditorViewModel

    // 当前选择的背景音乐
    private var selectedMusicInfo: MediaInfo? = null

    // 原始音频音量（0.0-1.0）
    private var originalVolume: Float = 1.0f

    // 背景音乐音量（0.0-1.0）
    private var musicVolume: Float = 0.8f

    // 是否静音原始音频
    private var isMuteOriginal: Boolean = false

    // 是否循环播放背景音乐
    private var isLoopMusic: Boolean = false

    // 背景音乐裁剪起始时间（毫秒）
    private var musicTrimStartMs: Long = 0

    // 背景音乐裁剪结束时间（毫秒）
    private var musicTrimEndMs: Long = 0

    // 渐入时长（秒）
    private var fadeInDurationSec: Float = 0f

    // 渐出时长（秒）
    private var fadeOutDurationSec: Float = 0f

    // 媒体选择结果回调
    private val selectMusicLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getParcelableExtra<MediaInfo>(MediaManageActivity.RESULT_DATA)
                ?.let { mediaInfo ->
                    if (mediaInfo.isAudio() || mediaInfo.isVideo()) {
                        // 设置选中的背景音乐
                        selectedMusicInfo = mediaInfo
                        binding.tvMusicName.text = File(mediaInfo.path).name

                        // 设置音乐时长
                        musicTrimEndMs = mediaInfo.duration
                        updateMusicTrimUI()
                    } else {
                        context?.toastShort("请选择音频文件")
                    }
                }
        }
    }

    companion object {
        fun newInstance(): AudioPanelFragment {
            return AudioPanelFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PanelAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取ViewModel
        viewModel = ViewModelProvider(requireActivity())[VideoEditorViewModel::class.java]

        // 初始化UI
        initUI()

        // 初始化监听器
        initListeners()

        // 恢复之前的设置（如果有）
        restorePreviousSettings()
    }

    /**
     * 初始化UI
     */
    private fun initUI() {
        // 设置原始音量显示
        binding.tvOriginalVolume.text = "${(originalVolume * 100).toInt()}%"
        binding.seekBarOriginalVolume.progress = (originalVolume * 100).toInt()

        // 设置背景音乐音量显示
        binding.tvMusicVolume.text = "${(musicVolume * 100).toInt()}%"
        binding.seekBarMusicVolume.progress = (musicVolume * 100).toInt()

        // 设置开关状态
        binding.switchMuteOriginal.isChecked = isMuteOriginal
        binding.switchLoopMusic.isChecked = isLoopMusic

        // 设置渐入渐出初始值
        binding.tvFadeInValue.text = String.format(Locale.getDefault(), "%.1fs", fadeInDurationSec)
        binding.tvFadeOutValue.text =
            String.format(Locale.getDefault(), "%.1fs", fadeOutDurationSec)
        binding.seekBarFadeIn.progress = (fadeInDurationSec * 10).toInt()
        binding.seekBarFadeOut.progress = (fadeOutDurationSec * 10).toInt()

        // 更新音乐裁剪UI
        updateMusicTrimUI()
    }

    /**
     * 初始化监听器
     */
    private fun initListeners() {
        // 原始音频音量调整
        binding.seekBarOriginalVolume.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                originalVolume = progress / 100f
                binding.tvOriginalVolume.text = "$progress%"

                // 预览音量变化
                viewModel.setOriginalVolume(originalVolume)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 背景音乐音量调整
        binding.seekBarMusicVolume.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                musicVolume = progress / 100f
                binding.tvMusicVolume.text = "$progress%"

                // 预览音量变化
                viewModel.setMusicVolume(musicVolume)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 原始音频静音开关
        binding.switchMuteOriginal.setOnCheckedChangeListener { _, isChecked ->
            isMuteOriginal = isChecked

            // 预览静音效果
            if (isChecked) {
                viewModel.setOriginalVolume(0f)
            } else {
                viewModel.setOriginalVolume(originalVolume)
            }
        }

        // 背景音乐循环开关
        binding.switchLoopMusic.setOnCheckedChangeListener { _, isChecked ->
            isLoopMusic = isChecked
            viewModel.setMusicLoop(isChecked)
        }

        // 选择背景音乐按钮
        binding.btnSelectMusic.setOnClickListener {
            // 打开媒体选择Activity
            selectMusicLauncher.launch(
                MediaManageActivity.getIntent(
                    requireActivity() as AppCompatActivity,
                    MediaConfig(MediaConfig.MEDIA_TYPE_AUDIO, originalMedia = true)
                )
            )
        }

        // 背景音乐裁剪进度条
        binding.seekBarMusicTrim.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedMusicInfo?.let { music ->
                    // 计算裁剪位置
                    val position = (progress / 100f) * music.duration
                    musicTrimStartMs = position.toLong()

                    // 更新显示
                    updateMusicTrimUI()

                    // 预览裁剪效果
                    viewModel.previewMusicTrim(musicTrimStartMs, musicTrimEndMs)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 渐入时长调整
        binding.seekBarFadeIn.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fadeInDurationSec = progress / 10f
                binding.tvFadeInValue.text =
                    String.format(Locale.getDefault(), "%.1fs", fadeInDurationSec)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 渐出时长调整
        binding.seekBarFadeOut.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fadeOutDurationSec = progress / 10f
                binding.tvFadeOutValue.text =
                    String.format(Locale.getDefault(), "%.1fs", fadeOutDurationSec)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 提取音频按钮
        binding.btnExtractAudio.setOnClickListener {
            extractAudio()
        }

        // 应用音频设置按钮
        binding.btnApplyAudio.setOnClickListener {
            applyAudioSettings()
        }
    }

    /**
     * 恢复之前的设置
     */
    private fun restorePreviousSettings() {
        // 从ViewModel获取当前的音频设置
        viewModel.getAudioSettings()?.let { settings ->
            // 恢复原始音频设置
            originalVolume = settings.originalVolume
            isMuteOriginal = settings.isMuteOriginal

            // 恢复背景音乐设置
            settings.backgroundMusic?.let { musicInfo ->
                selectedMusicInfo = musicInfo
                binding.tvMusicName.text = File(musicInfo.path).name
                musicTrimStartMs = settings.musicTrimStartMs
                musicTrimEndMs = settings.musicTrimEndMs
                musicVolume = settings.musicVolume
                isLoopMusic = settings.isLoopMusic
                fadeInDurationSec = settings.fadeInDurationSec
                fadeOutDurationSec = settings.fadeOutDurationSec
            }

            // 更新UI
            initUI()
        }
    }

    /**
     * 更新音乐裁剪UI
     */
    private fun updateMusicTrimUI() {
        selectedMusicInfo?.let { music ->
            // 更新开始和结束时间显示
            binding.tvMusicStartTime.text = formatDuration(musicTrimStartMs)
            binding.tvMusicEndTime.text = formatDuration(music.duration)
        } ?: run {
            // 没有选择音乐时显示默认值
            binding.tvMusicStartTime.text = "00:00"
            binding.tvMusicEndTime.text = "00:00"
        }
    }

    /**
     * 提取音频
     */
    private fun extractAudio() {
        // 获取当前编辑的视频
        val currentMedia = viewModel.getCurrentMedia()
        if (currentMedia == null) {
            context?.toastShort("没有可用的视频源")
            return
        }

        // 调用ViewModel执行音频提取
        viewModel.extractAudio(requireContext())
        context?.toastLong("音频提取任务已开始，完成后将保存到下载目录")
    }

    /**
     * 应用音频设置
     */
    private fun applyAudioSettings() {
        // 创建音频编辑操作
        val audioOperation = AudioEditOperation(
            originalVolume = if (isMuteOriginal) 0f else originalVolume,
            isMuteOriginal = isMuteOriginal,
            backgroundMusic = selectedMusicInfo,
            musicVolume = musicVolume,
            musicTrimStartMs = musicTrimStartMs,
            musicTrimEndMs = musicTrimEndMs,
            isLoopMusic = isLoopMusic,
            fadeInDurationSec = fadeInDurationSec,
            fadeOutDurationSec = fadeOutDurationSec
        )

        // 应用到ViewModel
        viewModel.applyAudioOperation(audioOperation)

        // 显示提示
        context?.toastShort("音频设置已应用")
    }

    override fun onPause() {
        super.onPause()
        // 在Fragment暂停时自动保存设置
        applyAudioSettings()
    }
} 