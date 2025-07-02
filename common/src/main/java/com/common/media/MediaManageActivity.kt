package com.common.media

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.common.common.databinding.ActivityMediaManageBinding
import com.common.kt.activity.requestPermission
import com.common.kt.activity.toIntent
import com.common.kt.setTitleBarContent
import com.common.kt.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaManageActivity : AppCompatActivity() {
    private val viewBinding by viewBinding<ActivityMediaManageBinding>()
    private val mediaViewModel by viewModels<MediaManagerViewModel>()
    private val mediaConfig by lazy { intent.getParcelableExtra(BUNDLE_DATA) ?: MediaConfig() }
    private val mediaAdapter by lazy { MediaManageAdapter(mediaConfig.enableMultiSelect) }

    // 保存已选择的媒体列表
    private val selectedMediaList = mutableListOf<MediaInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置标题
        title = when (mediaConfig.mediaType) {
            MediaConfig.MEDIA_TYPE_IMAGE -> "选择图片"
            MediaConfig.MEDIA_TYPE_VIDEO -> "选择视频"
            MediaConfig.MEDIA_TYPE_AUDIO -> "选择音频"
            else -> "选择媒体"
        }
        setTitleBarContent(title, if (mediaConfig.enableMultiSelect) "完成" else "", {
            finish()
        }) {
            finishWithMultiSelectResult()
        }

        viewBinding.apply {
            rvMedia.adapter = mediaAdapter

            // 显示已选数量的提示
            if (mediaConfig.enableMultiSelect) {
                tvSelectedCount.text = "已选择: 0/${mediaConfig.maxSelectCount}"
                tvSelectedCount.visibility = android.view.View.VISIBLE
            } else {
                tvSelectedCount.visibility = android.view.View.GONE
            }
        }

        // 设置项目点击监听器
        mediaAdapter.setOnItemClickListener { adapter, view, position ->
            val mediaInfo = adapter.getItem(position) ?: return@setOnItemClickListener

            if (mediaConfig.enableMultiSelect) {
                // 多选模式处理
                handleMultiSelect(mediaInfo)
            } else {
                // 单选模式处理
                handleSingleSelect(mediaInfo)
            }
        }

        mediaViewModel.mediaSources.observe(this) {
            mediaAdapter.submitList(it)
        }

        requestPermission {
            mediaViewModel.getMediaSource(mediaConfig)
        }
    }

    /**
     * 处理多选逻辑
     */
    private fun handleMultiSelect(mediaInfo: MediaInfo) {
        // 切换选中状态
        mediaInfo.isSelect = !mediaInfo.isSelect

        if (mediaInfo.isSelect) {
            // 检查是否超过最大选择数量
            if (selectedMediaList.size >= mediaConfig.maxSelectCount) {
                mediaInfo.isSelect = false // 恢复未选中状态
                Toast.makeText(
                    this,
                    "最多只能选择${mediaConfig.maxSelectCount}个文件",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            // 添加到已选列表
            selectedMediaList.add(mediaInfo)
        } else {
            // 从已选列表移除
            selectedMediaList.remove(mediaInfo)
        }

        // 更新选择数量显示
        viewBinding.tvSelectedCount.text =
            "已选择: ${selectedMediaList.size}/${mediaConfig.maxSelectCount}"

        // 通知适配器更新项目
        mediaAdapter.notifyDataSetChanged()
    }

    /**
     * 处理单选逻辑
     */
    private fun handleSingleSelect(mediaInfo: MediaInfo) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (mediaConfig.originalMedia) {
                setResult(RESULT_OK, intent.putExtra(RESULT_DATA, mediaInfo))
                finish()
                return@launch
            }
            if (mediaConfig.mediaType == MediaConfig.MEDIA_TYPE_IMAGE) {
                mediaViewModel.getCompressImagePath(mediaInfo.path)?.let {
                    mediaInfo.path = it
                    setResult(RESULT_OK, intent.putExtra(RESULT_DATA, mediaInfo))
                    finish()
                } ?: run {
                    // 压缩失败时，使用原图
                    setResult(RESULT_OK, intent.putExtra(RESULT_DATA, mediaInfo))
                    finish()
                }
            } else {
                setResult(RESULT_OK, intent.putExtra(RESULT_DATA, mediaInfo))
                finish()
            }
        }
    }

    /**
     * 完成多选并返回结果
     */
    private fun finishWithMultiSelectResult() {
        if (selectedMediaList.isEmpty()) {
            Toast.makeText(this, "请至少选择一个文件", Toast.LENGTH_SHORT).show()
            return
        }

        // 处理已选媒体文件
        lifecycleScope.launch(Dispatchers.IO) {
            val resultList =
                if (mediaConfig.originalMedia || mediaConfig.mediaType != MediaConfig.MEDIA_TYPE_IMAGE) {
                    // 直接使用原始文件
                    selectedMediaList
                } else {
                    // 图片需要压缩
                    val compressedList = mutableListOf<MediaInfo>()
                    for (mediaInfo in selectedMediaList) {
                        val compressedPath = mediaViewModel.getCompressImagePath(mediaInfo.path)
                        if (compressedPath != null) {
                            // 创建新的MediaInfo对象，保留原始对象不变
                            compressedList.add(mediaInfo.copy(path = compressedPath))
                        } else {
                            // 压缩失败时使用原图
                            compressedList.add(mediaInfo)
                        }
                    }
                    compressedList
                }

            // 返回结果列表
            val resultIntent =
                Intent().putParcelableArrayListExtra(RESULT_LIST_DATA, ArrayList(resultList))
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }


    companion object {
        const val BUNDLE_DATA = "BUNDLE_DATA"
        const val RESULT_DATA = "RESULT_DATA"
        const val RESULT_LIST_DATA = "RESULT_LIST_DATA"

        fun getIntent(
            activity: AppCompatActivity,
            mediaConfig: MediaConfig
        ): Intent {
            return activity.toIntent<MediaManageActivity>(BUNDLE_DATA to mediaConfig)
        }
    }
}