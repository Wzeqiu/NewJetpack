package com.wzeqiu.mediacode.editor

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.TextureOverlay
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.common.kt.toastLong
import com.common.kt.toastShort
import com.common.media.MediaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 视频编辑器ViewModel
 * 
 * 负责处理视频编辑相关的业务逻辑和数据管理，包括：
 * - 媒体文件加载和播放控制
 * - 编辑操作（剪辑、滤镜、特效等）
 * - 编辑状态管理
 * - 导出处理
 */
class VideoEditorViewModel : ViewModel() {

    companion object {
        private const val TAG = "VideoEditorViewModel"
    }

    // 播放器实例
    private var player: ExoPlayer? = null
    
    // 当前编辑的媒体信息
    private var currentMedia: MediaInfo? = null
    
    // 编辑操作列表（用于记录所有编辑操作，支持撤销/重做）
    private val editOperations = mutableListOf<EditOperation>()
    
    // 裁剪起始时间（毫秒）
    private var trimStartMs: Long = 0
    
    // 裁剪结束时间（毫秒）
    private var trimEndMs: Long = -1
    
    // 播放速度
    private var playbackSpeed: Float = 1.0f
    
    // 旋转角度（度）
    private var rotationDegrees: Float = 0f
    
    // 是否水平翻转
    private var isFlippedHorizontally: Boolean = false
    
    // 是否垂直翻转
    private var isFlippedVertically: Boolean = false
    
    // 音量大小（0.0-1.0）
    private var volume: Float = 1.0f
    
    // 应用的滤镜效果
    private var currentFilter: VideoFilter? = null
    
    // 当前音频设置
    private var currentAudioOperation: AudioEditOperation? = null
    
    // 背景音乐播放器
    private var musicPlayer: ExoPlayer? = null
    
    // 文字覆盖层列表
    private val textOverlays = mutableListOf<TextOverlay>()
    
    // 文字覆盖层LiveData
    private val _textOverlayList = MutableLiveData<List<TextOverlay>>()
    val textOverlayList: LiveData<List<TextOverlay>> = _textOverlayList
    
    // 贴纸覆盖层列表
    private val stickerOverlays = mutableListOf<StickerOverlay>()
    
    // 贴纸覆盖层LiveData
    private val _stickerOverlayList = MutableLiveData<List<StickerOverlay>>()
    val stickerOverlayList: LiveData<List<StickerOverlay>> = _stickerOverlayList
    
    // 当前选中的贴纸覆盖层
    private val _selectedStickerOverlay = MutableLiveData<StickerOverlay?>()
    val selectedStickerOverlay: LiveData<StickerOverlay?> = _selectedStickerOverlay
    
    // 编辑进度（用于显示各种操作的进度）
    private val _editorProgress = MutableLiveData<Int>()
    val editorProgress: LiveData<Int> = _editorProgress
    
    // 导出状态
    private val _exportStatus = MutableLiveData<ExportStatus>()
    val exportStatus: LiveData<ExportStatus> = _exportStatus
    
    // 播放位置（毫秒）
    private val _playbackPosition = MutableLiveData<Long>()
    val playbackPosition: LiveData<Long> = _playbackPosition
    
    // 媒体时长（毫秒）
    private val _mediaDuration = MutableLiveData<Long>()
    val mediaDuration: LiveData<Long> = _mediaDuration
    
    // 播放状态
    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying
    
    /**
     * 获取裁剪起始时间（毫秒）
     */
    fun getTrimStartMs(): Long {
        return trimStartMs
    }
    
    /**
     * 获取裁剪结束时间（毫秒）
     */
    fun getTrimEndMs(): Long {
        return trimEndMs
    }
    
    /**
     * 加载媒体文件
     * 
     * @param mediaInfo 要加载的媒体信息
     */
    fun loadMedia(mediaInfo: MediaInfo) {
        currentMedia = mediaInfo
        
        _mediaDuration.value = mediaInfo.duration
        trimStartMs = 0
        trimEndMs = mediaInfo.duration
        
        // 初始化播放器
        initializePlayer(mediaInfo)
    }
    
    /**
     * 初始化播放器
     */
    private fun initializePlayer(mediaInfo: MediaInfo) {
        viewModelScope.launch(Dispatchers.Main) {
            player?.release()
            
            // 创建ExoPlayer实例
            withContext(Dispatchers.Main) {
                val context = com.blankj.utilcode.util.Utils.getApp()
                player = ExoPlayer.Builder(context).build().apply {
                    // 设置媒体源
                    val mediaItem = MediaItem.fromUri(Uri.parse(mediaInfo.path))
                    setMediaItem(mediaItem)
                    
                    // 设置播放监听
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            _isPlaying.postValue(state == Player.STATE_READY && isPlaying)
                        }
                        
                        override fun onPositionDiscontinuity(
                            oldPosition: Player.PositionInfo,
                            newPosition: Player.PositionInfo,
                            reason: Int
                        ) {
                            _playbackPosition.postValue(newPosition.positionMs)
                        }
                    })
                    
                    // 准备播放器
                    prepare()
                }
            }
        }
    }
    
    /**
     * 播放/暂停控制
     */
    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
            _isPlaying.value = it.isPlaying
        }
    }
    
    /**
     * 设置播放位置
     * 
     * @param positionMs 播放位置（毫秒）
     */
    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }
    
    /**
     * 设置裁剪范围
     * 
     * @param startMs 起始时间（毫秒）
     * @param endMs 结束时间（毫秒）
     */
    fun setTrimRange(startMs: Long, endMs: Long) {
        trimStartMs = startMs
        trimEndMs = endMs
        
        // 记录编辑操作
        addEditOperation(EditOperation.Trim(startMs, endMs))
    }
    
    /**
     * 设置播放速度
     * 
     * @param speed 播放速度（0.25-2.0）
     */
    fun setPlaybackSpeed(speed: Float) {
        val limitedSpeed = speed.coerceIn(0.25f, 2.0f)
        playbackSpeed = limitedSpeed
        player?.setPlaybackSpeed(limitedSpeed)
        
        // 记录编辑操作
        addEditOperation(EditOperation.Speed(limitedSpeed))
    }
    
    /**
     * 设置旋转角度
     * 
     * @param degrees 旋转角度（度）
     */
    fun setRotation(degrees: Float) {
        rotationDegrees = degrees
        
        // 记录编辑操作
        addEditOperation(EditOperation.Rotation(degrees))
    }
    
    /**
     * 设置水平翻转
     * 
     * @param flipped 是否水平翻转
     */
    fun setFlipHorizontal(flipped: Boolean) {
        isFlippedHorizontally = flipped
        
        // 记录编辑操作
        addEditOperation(EditOperation.Flip(horizontal = flipped, vertical = isFlippedVertically))
    }
    
    /**
     * 设置垂直翻转
     * 
     * @param flipped 是否垂直翻转
     */
    fun setFlipVertical(flipped: Boolean) {
        isFlippedVertically = flipped
        
        // 记录编辑操作
        addEditOperation(EditOperation.Flip(horizontal = isFlippedHorizontally, vertical = flipped))
    }
    
    /**
     * 设置音量
     * 
     * @param volume 音量大小（0.0-1.0）
     */
    fun setVolume(volume: Float) {
        this.volume = volume.coerceIn(0f, 1f)
        player?.volume = this.volume
        
        // 记录编辑操作
        addEditOperation(EditOperation.Volume(this.volume))
    }
    
    /**
     * 应用滤镜
     * 
     * @param filter 滤镜效果
     */
    fun applyFilter(filter: VideoFilter) {
        currentFilter = filter
        
        // 记录编辑操作
        addEditOperation(EditOperation.Filter(filter))
    }
    
    /**
     * 添加编辑操作
     */
    private fun addEditOperation(operation: EditOperation) {
        editOperations.add(operation)
        // 这里可以实现撤销/重做功能
    }
    
    /**
     * 撤销上一步操作
     */
    fun undo() {
        if (editOperations.isNotEmpty()) {
            editOperations.removeAt(editOperations.size - 1)
            // 重新应用所有剩余的编辑操作
            applyAllEditOperations()
        }
    }
    
    /**
     * 重新应用所有编辑操作
     */
    private fun applyAllEditOperations() {
        // 重置所有编辑参数
        resetEditParams()
        
        // 重新应用所有操作
        for (operation in editOperations) {
            when (operation) {
                is EditOperation.Trim -> {
                    trimStartMs = operation.startMs
                    trimEndMs = operation.endMs
                }
                is EditOperation.Speed -> {
                    playbackSpeed = operation.speed
                    player?.setPlaybackSpeed(operation.speed)
                }
                is EditOperation.Rotation -> {
                    rotationDegrees = operation.degrees
                }
                is EditOperation.Flip -> {
                    isFlippedHorizontally = operation.horizontal
                    isFlippedVertically = operation.vertical
                }
                is EditOperation.Volume -> {
                    volume = operation.volume
                    player?.volume = volume
                }
                is EditOperation.Filter -> {
                    currentFilter = operation.filter
                }
                is EditOperation.Audio -> {
                    currentAudioOperation = operation.audioOperation
                    
                    // 设置原始音频音量
                    val audioOp = operation.audioOperation
                    setOriginalVolume(if (audioOp.isMuteOriginal) 0f else audioOp.originalVolume)
                    
                    // 处理背景音乐
                    audioOp.backgroundMusic?.let { musicInfo ->
                        // 初始化背景音乐播放器
                        if (musicPlayer == null) {
                            initMusicPlayer(musicInfo)
                        }
                        
                        // 设置音量
                        musicPlayer?.volume = audioOp.musicVolume
                        
                        // 设置循环模式
                        setMusicLoop(audioOp.isLoopMusic)
                        
                        // 设置播放位置
                        musicPlayer?.seekTo(audioOp.musicTrimStartMs)
                    }
                }
                is EditOperation.Text -> {
                    // 添加文字覆盖层
                    val textOverlay = TextOverlay(
                        text = operation.text,
                        startTimeMs = operation.startTimeMs,
                        endTimeMs = operation.endTimeMs,
                        xPosition = operation.x,
                        yPosition = operation.y,
                        fontSize = operation.fontSize,
                        textColor = operation.color
                    )
                    textOverlays.add(textOverlay)
                }
                is EditOperation.Sticker -> {
                    // 添加贴纸覆盖层
                    stickerOverlays.add(operation.stickerOverlay)
                }
            }
        }
    }
    
    /**
     * 重置编辑参数
     */
    private fun resetEditParams() {
        trimStartMs = 0
        trimEndMs = currentMedia?.duration ?: -1
        playbackSpeed = 1.0f
        rotationDegrees = 0f
        isFlippedHorizontally = false
        isFlippedVertically = false
        volume = 1.0f
        currentFilter = null
        textOverlays.clear()
        _textOverlayList.value = emptyList()
        stickerOverlays.clear()
        _stickerOverlayList.value = emptyList()
        currentAudioOperation = null
        
        player?.setPlaybackSpeed(1.0f)
        player?.volume = 1.0f
    }
    
    /**
     * 导出媒体文件
     */
    @OptIn(UnstableApi::class)
    fun exportMedia(context: Context) {
        viewModelScope.launch {
            try {
                val mediaInfo = currentMedia ?: return@launch
                
                // 更新导出状态
                _exportStatus.value = ExportStatus.Preparing
                
                // 创建输出文件
                val outputFile = createOutputFile(context, mediaInfo)
                
                // 准备导出参数
                val mediaItem = MediaItem.fromUri(Uri.parse(mediaInfo.path))
                
                // 创建编辑后的媒体项
                val editedMediaItemBuilder = EditedMediaItem.Builder(mediaItem)
                
                // 应用裁剪
                if (trimStartMs > 0 || trimEndMs < mediaInfo.duration) {
                    editedMediaItemBuilder.setRemoveAudio(false)
                    mediaItem.buildUpon()
                        .setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(trimStartMs)
                                .setEndPositionMs(trimEndMs)
                                .build()
                        )
                        .build()
                }
                
                // 创建效果列表
                val effects = mutableListOf<androidx.media3.common.Effect>()
                
                // 应用旋转和翻转
                if (rotationDegrees != 0f || isFlippedHorizontally || isFlippedVertically) {
                    val scaleX = if (isFlippedHorizontally) -1f else 1f
                    val scaleY = if (isFlippedVertically) -1f else 1f
                    
                    val transformation = ScaleAndRotateTransformation.Builder()
                        .setRotationDegrees(rotationDegrees)
                        .setScale(scaleX, scaleY)
                        .build()
                    
                    effects.add(transformation)
                }
                
                // 应用滤镜效果
                currentFilter?.let { filter ->
                    // 此处应用滤镜效果
                }
                
                // 应用文字覆盖
                if (textOverlays.isNotEmpty()) {
                    try {
                        // 将文字覆盖层转换为Media3的TextureOverlay
                        val textEffects = createTextOverlayEffects()
                        effects.addAll(textEffects)
                    } catch (e: Exception) {
                        Log.e(TAG, "添加文字覆盖层失败", e)
                    }
                }
                
                // 应用贴纸覆盖
                if (stickerOverlays.isNotEmpty()) {
                    try {
                        // 将贴纸覆盖层转换为Media3的TextureOverlay
                        val stickerEffects = createStickerOverlayEffects()
                        effects.addAll(stickerEffects)
                    } catch (e: Exception) {
                        Log.e(TAG, "添加贴纸覆盖层失败", e)
                    }
                }
                
                // 应用所有效果
                if (effects.isNotEmpty()) {
                    val effects = Effects(listOf(/* audioProcessors */), effects)
                    editedMediaItemBuilder.setEffects(effects)
                }
                
                val editedMediaItem = editedMediaItemBuilder.build()
                
                // 创建导出器
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            result: ExportResult
                        ) {
                            _exportStatus.postValue(ExportStatus.Completed(outputFile.absolutePath))
                        }
                        
                        override fun onError(
                            composition: Composition,
                            result: ExportResult,
                            exception: ExportException
                        ) {
                            _exportStatus.postValue(ExportStatus.Error(exception.message ?: "导出失败"))
                        }
                    })
                    .build()
                
                // 开始导出
                _exportStatus.value = ExportStatus.Processing(0)
                transformer.start(editedMediaItem, outputFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "导出媒体文件失败", e)
                _exportStatus.value = ExportStatus.Error(e.message ?: "导出过程中发生异常")
            }
        }
    }
    
    /**
     * 创建输出文件
     */
    private fun createOutputFile(context: Context, mediaInfo: MediaInfo): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "EDITED_${timeStamp}.${mediaInfo.getExtension()}"
        val outputDir = File(context.getExternalFilesDir(null), "edited_media")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        return File(outputDir, fileName)
    }
    
    /**
     * 释放资源
     */
    fun releaseResources() {
        player?.release()
        player = null
        
        musicPlayer?.release()
        musicPlayer = null
    }
    
    /**
     * 获取当前编辑的媒体信息
     */
    fun getCurrentMedia(): MediaInfo? {
        return currentMedia
    }
    
    /**
     * 获取当前音频设置
     */
    fun getAudioSettings(): AudioEditOperation? {
        return currentAudioOperation
    }
    
    /**
     * 设置原始音频音量
     * 
     * @param volume 音量大小（0.0-1.0）
     */
    fun setOriginalVolume(volume: Float) {
        this.volume = volume
        player?.volume = volume
    }
    
    /**
     * 设置背景音乐音量
     * 
     * @param volume 音量大小（0.0-1.0）
     */
    fun setMusicVolume(volume: Float) {
        musicPlayer?.volume = volume
    }
    
    /**
     * 设置背景音乐循环播放
     * 
     * @param loop 是否循环播放
     */
    fun setMusicLoop(loop: Boolean) {
        musicPlayer?.repeatMode = if (loop) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
    }
    
    /**
     * 预览背景音乐裁剪效果
     * 
     * @param startMs 开始时间（毫秒）
     * @param endMs 结束时间（毫秒）
     */
    fun previewMusicTrim(startMs: Long, endMs: Long) {
        currentAudioOperation?.backgroundMusic?.let { musicInfo ->
            // 初始化背景音乐播放器
            if (musicPlayer == null) {
                initMusicPlayer(musicInfo)
            }
            
            // 设置裁剪范围
            musicPlayer?.seekTo(startMs)
        }
    }
    
    /**
     * 初始化背景音乐播放器
     */
    private fun initMusicPlayer(musicInfo: MediaInfo) {
        val context = com.blankj.utilcode.util.Utils.getApp()
        
        // 释放旧的播放器
        musicPlayer?.release()
        
        // 创建新的播放器
        musicPlayer = ExoPlayer.Builder(context).build().apply {
            // 设置音频属性
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            
            // 设置媒体源
            val mediaItem = MediaItem.fromUri(Uri.parse(musicInfo.path))
            setMediaItem(mediaItem)
            
            // 准备播放器
            prepare()
        }
    }
    
    /**
     * 应用音频编辑操作
     * 
     * @param operation 音频编辑操作
     */
    fun applyAudioOperation(operation: AudioEditOperation) {
        // 保存音频设置
        currentAudioOperation = operation
        
        // 设置原始音频音量
        setOriginalVolume(if (operation.isMuteOriginal) 0f else operation.originalVolume)
        
        // 处理背景音乐
        operation.backgroundMusic?.let { musicInfo ->
            // 初始化背景音乐播放器
            if (musicPlayer == null) {
                initMusicPlayer(musicInfo)
            }
            
            // 设置音量
            musicPlayer?.volume = operation.musicVolume
            
            // 设置循环模式
            setMusicLoop(operation.isLoopMusic)
            
            // 设置播放位置
            musicPlayer?.seekTo(operation.musicTrimStartMs)
        }
        
        // 记录编辑操作
        addEditOperation(EditOperation.Audio(operation))
    }
    
    /**
     * 提取音频
     * 
     * @param context 上下文
     */
    @OptIn(UnstableApi::class)
    fun extractAudio(context: Context) {
        currentMedia?.let { mediaInfo ->
            viewModelScope.launch {
                try {
                    // 创建输出文件
                    val fileName = "audio_${System.currentTimeMillis()}.mp3"
                    val outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val outputFile = File(outputDir, fileName)
                    
                    // 确保目录存在
                    outputDir.mkdirs()
                    
                    // 创建转换器
                    val transformer = Transformer.Builder(context)
                        .setAudioMimeType(MimeTypes.AUDIO_MPEG) // 输出为MP3格式
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, result: ExportResult) {
                                _editorProgress.postValue(100)
                                context.toastLong("音频提取完成，已保存到: ${outputFile.path}")
                            }
                            
                            override fun onError(
                                composition: Composition,
                                result: ExportResult,
                                exception: ExportException
                            ) {
                                _editorProgress.postValue(0)
                                context.toastShort("音频提取失败: ${exception.message}")
                            }
                        })
                        .build()
                    
                    // 创建编辑媒体项
                    val mediaItem = MediaItem.fromUri(Uri.parse(mediaInfo.path))
                    val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                        .setRemoveVideo(true) // 只保留音频
                        .build()
                    
                    // 开始转换
                    _editorProgress.postValue(0)
                    transformer.start(editedMediaItem, outputFile.path)
                } catch (e: Exception) {
                    _editorProgress.postValue(0)
                    context.toastShort("音频提取异常: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 添加文字覆盖层
     * 
     * @param textOverlay 文字覆盖层对象
     */
    fun addTextOverlay(textOverlay: TextOverlay) {
        textOverlays.add(textOverlay)
        _textOverlayList.value = textOverlays.toList()
        
        // 记录编辑操作
        addEditOperation(EditOperation.Text(
            text = textOverlay.text,
            startTimeMs = textOverlay.startTimeMs,
            endTimeMs = textOverlay.endTimeMs,
            x = textOverlay.xPosition,
            y = textOverlay.yPosition,
            fontSize = textOverlay.fontSize,
            color = textOverlay.textColor
        ))
    }
    
    /**
     * 移除文字覆盖层
     * 
     * @param textOverlay 要移除的文字覆盖层
     */
    fun removeTextOverlay(textOverlay: TextOverlay) {
        textOverlays.remove(textOverlay)
        _textOverlayList.value = textOverlays.toList()
    }
    
    /**
     * 获取所有文字覆盖层
     * 
     * @return 文字覆盖层列表
     */
    fun getTextOverlays(): List<TextOverlay> {
        return textOverlays.toList()
    }
    
    /**
     * 获取当前时间点应该显示的文字覆盖层
     * 
     * @param timeMs 当前时间（毫秒）
     * @return 应该显示的文字覆盖层列表
     */
    fun getVisibleTextOverlays(timeMs: Long): List<TextOverlay> {
        return textOverlays.filter { 
            timeMs in it.startTimeMs until it.endTimeMs 
        }
    }
    
    /**
     * 添加贴纸覆盖层
     * 
     * @param stickerOverlay 贴纸覆盖层对象
     */
    fun addStickerOverlay(stickerOverlay: StickerOverlay) {
        stickerOverlays.add(stickerOverlay)
        _stickerOverlayList.value = stickerOverlays.toList()
        
        // 记录编辑操作
        addEditOperation(EditOperation.Sticker(stickerOverlay))
    }
    
    /**
     * 移除贴纸覆盖层
     * 
     * @param stickerOverlay 要移除的贴纸覆盖层
     */
    fun removeStickerOverlay(stickerOverlay: StickerOverlay) {
        stickerOverlays.remove(stickerOverlay)
        _stickerOverlayList.value = stickerOverlays.toList()
    }
    
    /**
     * 获取所有贴纸覆盖层
     * 
     * @return 贴纸覆盖层列表
     */
    fun getStickerOverlays(): List<StickerOverlay> {
        return stickerOverlays.toList()
    }
    
    /**
     * 获取当前时间点应该显示的贴纸覆盖层
     * 
     * @param timeMs 当前时间（毫秒）
     * @return 应该显示的贴纸覆盖层列表
     */
    fun getVisibleStickerOverlays(timeMs: Long): List<StickerOverlay> {
        return stickerOverlays.filter { 
            timeMs in it.startTimeMs until it.endTimeMs 
        }
    }
    
    /**
     * 选中贴纸覆盖层
     * 
     * @param stickerOverlay 要选中的贴纸覆盖层
     */
    fun selectStickerOverlay(stickerOverlay: StickerOverlay) {
        _selectedStickerOverlay.value = stickerOverlay
    }
    
    /**
     * 取消选中贴纸覆盖层
     */
    fun clearSelectedStickerOverlay() {
        _selectedStickerOverlay.value = null
    }
    
    /**
     * 更新贴纸覆盖层
     * 
     * @param updatedSticker 更新后的贴纸覆盖层
     */
    fun updateStickerOverlay(updatedSticker: StickerOverlay) {
        val index = stickerOverlays.indexOfFirst { 
            it.resourceId == updatedSticker.resourceId && 
            it.startTimeMs == updatedSticker.startTimeMs && 
            it.endTimeMs == updatedSticker.endTimeMs 
        }
        
        if (index != -1) {
            stickerOverlays[index] = updatedSticker
            _stickerOverlayList.value = stickerOverlays.toList()
            
            // 更新选中的贴纸
            if (_selectedStickerOverlay.value?.let { 
                it.resourceId == updatedSticker.resourceId && 
                it.startTimeMs == updatedSticker.startTimeMs && 
                it.endTimeMs == updatedSticker.endTimeMs 
            } == true) {
                _selectedStickerOverlay.value = updatedSticker
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        releaseResources()
    }
    
    /**
     * 编辑操作密封类
     */
    sealed class EditOperation {
        /**
         * 裁剪操作
         */
        data class Trim(val startMs: Long, val endMs: Long) : EditOperation()
        
        /**
         * 变速操作
         */
        data class Speed(val speed: Float) : EditOperation()
        
        /**
         * 旋转操作
         */
        data class Rotation(val degrees: Float) : EditOperation()
        
        /**
         * 翻转操作
         */
        data class Flip(val horizontal: Boolean, val vertical: Boolean) : EditOperation()
        
        /**
         * 音量操作
         */
        data class Volume(val volume: Float) : EditOperation()
        
        /**
         * 滤镜操作
         */
        data class Filter(val filter: VideoFilter) : EditOperation()
        
        /**
         * 音频操作
         */
        data class Audio(val audioOperation: AudioEditOperation) : EditOperation()
        
        /**
         * 文字操作
         */
        data class Text(
            val text: String,
            val startTimeMs: Long,
            val endTimeMs: Long,
            val x: Float,
            val y: Float,
            val fontSize: Float,
            val color: Int
        ) : EditOperation()
        
        /**
         * 贴纸操作
         */
        data class Sticker(val stickerOverlay: StickerOverlay) : EditOperation()
    }
    
    /**
     * 导出状态密封类
     */
    sealed class ExportStatus {
        /**
         * 准备中
         */
        object Preparing : ExportStatus()
        
        /**
         * 处理中
         */
        data class Processing(val progress: Int) : ExportStatus()
        
        /**
         * 已完成
         */
        data class Completed(val outputPath: String) : ExportStatus()
        
        /**
         * 出错
         */
        data class Error(val message: String) : ExportStatus()
    }

    /**
     * 创建文字覆盖效果
     * 
     * @return 文字覆盖效果列表
     */
    @OptIn(UnstableApi::class)
    private fun createTextOverlayEffects(): List<androidx.media3.common.Effect> {
        val effects = mutableListOf<androidx.media3.common.Effect>()
        
        // 获取当前媒体尺寸
        val mediaInfo = currentMedia ?: return emptyList()
        val videoWidth = mediaInfo.width.toFloat()
        val videoHeight = mediaInfo.height.toFloat()
        
        // 为每个文字覆盖层创建效果
//        for (overlay in textOverlays) {
//            // 创建文字绘制器
//            val textPainter = object : TextureOverlay.TextureOverlaySettings {
//                override fun drawFrame(
//                    canvas: android.graphics.Canvas,
//                    presentationTimeUs: Long
//                ): Boolean {
//                    // 检查时间范围
//                    val presentationTimeMs = presentationTimeUs / 1000
//                    if (presentationTimeMs < overlay.startTimeMs || presentationTimeMs > overlay.endTimeMs) {
//                        return false
//                    }
//
//                    // 创建画笔
//                    val paint = android.graphics.Paint().apply {
//                        color = overlay.textColor
//                        textSize = overlay.fontSize
//                        isAntiAlias = true
//                        textAlign = when (overlay.alignment) {
//                            0 -> android.graphics.Paint.Align.LEFT
//                            1 -> android.graphics.Paint.Align.CENTER
//                            2 -> android.graphics.Paint.Align.RIGHT
//                            else -> android.graphics.Paint.Align.CENTER
//                        }
//                    }
//
//                    // 计算位置
//                    val x = videoWidth * overlay.xPosition
//                    val y = videoHeight * overlay.yPosition
//
//                    // 绘制背景（如果需要）
//                    if (overlay.hasBackground) {
//                        val textBounds = android.graphics.Rect()
//                        paint.getTextBounds(overlay.text, 0, overlay.text.length, textBounds)
//
//                        val padding = overlay.fontSize * 0.2f
//                        val bgRect = android.graphics.RectF(
//                            x - textBounds.width() / 2 - padding,
//                            y - textBounds.height() - padding,
//                            x + textBounds.width() / 2 + padding,
//                            y + padding
//                        )
//
//                        val bgPaint = android.graphics.Paint().apply {
//                            color = overlay.backgroundColor
//                        }
//
//                        canvas.drawRect(bgRect, bgPaint)
//                    }
//
//                    // 绘制文字
//                    canvas.drawText(overlay.text, x, y, paint)
//
//                    return true
//                }
//            }
//
//            // 创建文字覆盖效果
//            val textOverlayEffect = TextureOverlay(textPainter)
//            effects.add(textOverlayEffect)
//        }
        
        return effects
    }

    /**
     * 创建贴纸覆盖效果
     * 
     * @return 贴纸覆盖效果列表
     */
    @OptIn(UnstableApi::class)
    private fun createStickerOverlayEffects(): List<androidx.media3.common.Effect> {
        val effects = mutableListOf<androidx.media3.common.Effect>()
        
        // 获取当前媒体尺寸
        val mediaInfo = currentMedia ?: return emptyList()
        val videoWidth = mediaInfo.width.toFloat()
        val videoHeight = mediaInfo.height.toFloat()
        
        // 为每个贴纸覆盖层创建效果
//        for (overlay in stickerOverlays) {
//            // 创建贴纸绘制器
//            val stickerPainter = object : TextureOverlay.TextureOverlaySettings {
//                override fun drawFrame(
//                    canvas: android.graphics.Canvas,
//                    presentationTimeUs: Long
//                ): Boolean {
//                    // 检查时间范围
//                    val presentationTimeMs = presentationTimeUs / 1000
//                    if (presentationTimeMs < overlay.startTimeMs || presentationTimeMs > overlay.endTimeMs) {
//                        return false
//                    }
//
//                    try {
//                        // 加载贴纸图像
//                        val context = com.blankj.utilcode.util.Utils.getApp()
//                        val bitmap = android.graphics.BitmapFactory.decodeResource(
//                            context.resources,
//                            overlay.resourceId
//                        )
//
//                        if (bitmap != null) {
//                            // 计算贴纸尺寸
//                            val stickerSize = minOf(videoWidth, videoHeight) * overlay.size
//                            val scaledWidth = stickerSize
//                            val scaledHeight = stickerSize * bitmap.height / bitmap.width
//
//                            // 计算位置
//                            val x = videoWidth * overlay.xPosition - scaledWidth / 2
//                            val y = videoHeight * overlay.yPosition - scaledHeight / 2
//
//                            // 设置透明度
//                            val paint = android.graphics.Paint().apply {
//                                alpha = overlay.alpha
//                            }
//
//                            // 创建矩阵进行旋转
//                            val matrix = android.graphics.Matrix()
//                            matrix.postTranslate(x, y)
//                            matrix.postRotate(
//                                overlay.rotation,
//                                x + scaledWidth / 2,
//                                y + scaledHeight / 2
//                            )
//
//                            // 创建目标矩形
//                            val dstRect = android.graphics.RectF(
//                                x, y, x + scaledWidth, y + scaledHeight
//                            )
//
//                            // 绘制贴纸
//                            canvas.drawBitmap(
//                                bitmap,
//                                null,
//                                dstRect,
//                                paint
//                            )
//
//                            // 释放资源
//                            bitmap.recycle()
//
//                            return true
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "贴纸绘制失败", e)
//                    }
//
//                    return false
//                }
//            }
//
//            // 创建纹理覆盖层效果
//            val textureOverlay = TextureOverlay(stickerPainter)
//            effects.add(textureOverlay)
//        }
        
        return effects
    }

    /**
     * 获取当前播放位置
     * 
     * @return 当前播放位置（毫秒）
     */
    fun getCurrentPosition(): Long {
        return player?.currentPosition ?: 0
    }
    
    /**
     * 获取媒体文件时长
     * 
     * @return 媒体文件时长（毫秒）
     */
    fun getMediaDuration(): Long {
        return currentMedia?.duration ?: 0
    }
}

/**
 * 视频滤镜类
 */
data class VideoFilter(
    val id: String,
    val name: String,
    val type: FilterType
) {
    /**
     * 滤镜类型枚举
     */
    enum class FilterType {
        COLOR,      // 颜色滤镜
        EFFECT,     // 特效滤镜
        BLUR,       // 模糊滤镜
        DISTORTION, // 失真滤镜
        CUSTOM      // 自定义滤镜
    }
} 