package com.wzeqiu.mediacode

import android.media.*
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * 一个使用MediaCodec裁剪音频和视频的类。
 * 支持精确的时间范围裁剪，并处理关键帧对齐问题。
 */
class MediaClipper {

    private val TAG = "MediaClipper"
    private val TIMEOUT_US = 10000L
    private var samplesWritten = false // 标记是否已写入任何样本

    /**
     * 在指定的开始和结束时间之间裁剪媒体文件。
     *
     * @param inputPath 输入媒体文件的路径。
     * @param outputPath 保存裁剪后媒体文件的路径。
     * @param startTimeUs 裁剪的开始时间（微秒）。
     * @param endTimeUs 裁剪的结束时间（微秒）。
     * @throws IOException 如果在裁剪过程中发生错误。
     */
    @Throws(IOException::class)
    fun clip(inputPath: String, outputPath: String, startTimeUs: Long, endTimeUs: Long) {
        Log.d(TAG, "开始裁剪: 输入=$inputPath, 输出=$outputPath, 开始=${startTimeUs}微秒, 结束=${endTimeUs}微秒")

        // 检查输入文件是否存在
        val inputFile = File(inputPath)
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw IOException("输入文件不存在或无法读取: $inputPath")
        }

        // 检查时间参数
        if (startTimeUs < 0 || endTimeUs <= startTimeUs) {
            throw IllegalArgumentException("无效的时间范围: 开始=${startTimeUs}微秒, 结束=${endTimeUs}微秒")
        }

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            samplesWritten = false // 重置此裁剪操作的标志
            extractor.setDataSource(inputPath)
            muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = selectTracks(extractor)
            val videoTrackIndex = trackMap["video"] ?: -1
            val audioTrackIndex = trackMap["audio"] ?: -1

            if (videoTrackIndex == -1 && audioTrackIndex == -1) {
                throw IOException("在输入文件中未找到视频或音频轨道。")
            }

            val muxerVideoTrackIndex = if (videoTrackIndex != -1) muxer.addTrack(extractor.getTrackFormat(videoTrackIndex)) else -1
            val muxerAudioTrackIndex = if (audioTrackIndex != -1) muxer.addTrack(extractor.getTrackFormat(audioTrackIndex)) else -1

            muxer.start()

            // --- 视频处理 ---
            if (videoTrackIndex != -1) {
                extractor.selectTrack(videoTrackIndex)
                // 在processTrack中完成seek操作
                processTrack(extractor, muxer, videoTrackIndex, muxerVideoTrackIndex, startTimeUs, endTimeUs, true)
                extractor.unselectTrack(videoTrackIndex)
            }

            // --- 音频处理 ---
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
                // 在processTrack中完成seek操作
                processTrack(extractor, muxer, audioTrackIndex, muxerAudioTrackIndex, startTimeUs, endTimeUs, false)
                extractor.unselectTrack(audioTrackIndex)
            }

            Log.d(TAG, "裁剪成功完成: $outputPath")

        } catch (e: Exception) {
            Log.e(TAG, "裁剪过程中出错", e)
            // 考虑在这里删除可能不完整的outputPath文件
            val outputFile = File(outputPath)
            if (outputFile.exists()) {
                outputFile.delete()
                Log.d(TAG, "已删除不完整的输出文件: $outputPath")
            }
            throw IOException("裁剪失败: ${e.message}", e)
        } finally {
            try {
                // 停止并释放muxer，特别处理stop()的错误
                try {
                    muxer?.stop()
                } catch (e: IllegalStateException) {
                    // 记录muxer停止失败的特定错误，如果没有写入样本或状态无效，这种情况很常见
                    Log.e(TAG, "无法停止muxer，可能是因为没有写入样本或状态无效。", e)
                } finally {
                    try {
                        muxer?.release()
                    } catch (e: Exception) {
                        Log.e(TAG, "释放muxer时出错", e)
                    }
                }

                // 释放extractor
                extractor.release()
            } catch (e: Exception) {
                // 捕获所有资源释放错误
                Log.e(TAG, "释放资源时出错", e)
            }
        }
    }

    /**
     * 选择视频和音频轨道。
     */
    private fun selectTracks(extractor: MediaExtractor): Map<String, Int> {
        val trackMap = mutableMapOf<String, Int>()
        var videoTrackIndex = -1
        var audioTrackIndex = -1
        val numTracks = extractor.trackCount
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true && videoTrackIndex == -1) {
                videoTrackIndex = i
                trackMap["video"] = i
                Log.d(TAG, "找到视频轨道: 索引=$i, 格式=$format")
            } else if (mime?.startsWith("audio/") == true && audioTrackIndex == -1) {
                audioTrackIndex = i
                trackMap["audio"] = i
                Log.d(TAG, "找到音频轨道: 索引=$i, 格式=$format")
            }
            if (videoTrackIndex != -1 && audioTrackIndex != -1) break
        }
        return trackMap
    }

    /**
     * 处理单个轨道的裁剪。
     */
    private fun processTrack(
        extractor: MediaExtractor,
        muxer: MediaMuxer,
        sourceTrackIndex: Int,
        muxerTrackIndex: Int,
        startTimeUs: Long,
        endTimeUs: Long,
        isVideo: Boolean
    ) {
        val bufferInfo = MediaCodec.BufferInfo()
        val buffer = ByteBuffer.allocate(1024 * 1024) // 根据需要调整缓冲区大小
        var presentationTimeUs: Long
        var firstSampleTime = -1L
        var frameCount = 0 // 添加帧计数器用于日志记录
        var firstWrittenSampleIsKey = false // 跟踪第一个写入的样本是否为关键帧
        var foundFirstKeyFrame = !isVideo // 音频不需要关键帧定位
        var actualStartTimeUs = startTimeUs // 使用请求的开始时间初始化，将为视频更新
        var seekSuccess = false // 标记seek操作是否成功

        Log.d(TAG, "处理轨道 $sourceTrackIndex (isVideo=$isVideo) 用于muxer轨道 $muxerTrackIndex, 时间范围: [$startTimeUs, $endTimeUs]")

        // 尝试不同的seek策略
        seekSuccess = trySeekStrategies(extractor, startTimeUs, sourceTrackIndex, isVideo)
        
        // 如果所有seek策略都失败，并且是视频轨道，记录错误但继续处理
        // 即使seek失败，我们也会尝试从当前位置读取并处理关键帧
        if (!seekSuccess && isVideo) {
            Log.e(TAG, "无法为视频轨道 $sourceTrackIndex 寻找到有效位置。将尝试使用第一个可用的关键帧。")
        }

        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) {
                Log.d(TAG, "轨道 $sourceTrackIndex 已到达流结束，共处理 $frameCount 帧。")
                break
            }

            presentationTimeUs = extractor.sampleTime
            val sampleFlags = extractor.sampleFlags

            // --- 视频关键帧定位逻辑 ---
            if (isVideo && !foundFirstKeyFrame) {
                // 检查是否为关键帧
                val isKeyFrame = (sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                Log.d(TAG, "处理视频帧: timeUs=$presentationTimeUs, isKeyFrame=$isKeyFrame")
                
                // 我们需要找到startTimeUs之后的第一个关键帧
                // 如果seek失败并且从头开始读取，可能需要处理文件开头的帧
                // 修改逻辑：如果seek失败，我们应该接受第一个关键帧，无论其时间戳如何
                if (isKeyFrame && (!seekSuccess || presentationTimeUs >= startTimeUs)) {
                    Log.d(TAG, "为视频轨道 $sourceTrackIndex 找到第一个关键帧: timeUs=$presentationTimeUs, seekSuccess=$seekSuccess")
                    foundFirstKeyFrame = true
                    actualStartTimeUs = presentationTimeUs // 调整实际开始时间为此关键帧的时间
                    firstSampleTime = presentationTimeUs // 标记这是我们处理的第一个样本时间
                } else {
                    // 跳过所需开始时间之前的帧或非关键帧
                    Log.d(TAG, "跳过视频帧: timeUs=$presentationTimeUs, isKeyFrame=$isKeyFrame, seekSuccess=$seekSuccess, startTimeUs=$startTimeUs")
                    extractor.advance() // 跳过这个不需要的帧
                    continue
                }
            } else {
                 // 对于音频，或找到关键帧后的视频，正常检查时间边界
                 if (presentationTimeUs < actualStartTimeUs) { // 现在使用actualStartTimeUs
                    extractor.advance() // 跳过这个不需要的帧
                    continue // 跳过（可能调整后的）开始时间之前的样本
                 }
                 if (presentationTimeUs > endTimeUs) {
                    Log.d(TAG, "轨道 $sourceTrackIndex 在 $presentationTimeUs > $endTimeUs 后的 $frameCount 帧达到结束时间。")
                    break // 结束时间后停止处理
                 }
                 if (firstSampleTime == -1L) {
                    // 如果在调整范围内，正常记录第一个样本时间
                    firstSampleTime = presentationTimeUs
                    Log.d(TAG, "轨道 $sourceTrackIndex 的时间范围内的第一个样本: timeUs=$firstSampleTime, flags=$sampleFlags")
                 }
            }

            // 如果我们还没有找到起点（例如，在找到视频关键帧之前开始的音频轨道），跳过写入
            if (firstSampleTime == -1L) {
                 extractor.advance() // 跳过这个不需要的帧
                 continue
            }

            // 相对于剪辑的*实际*开始（视频的关键帧时间）调整时间戳
            val adjustedTimeUs = presentationTimeUs - actualStartTimeUs
            if (adjustedTimeUs < 0) {
                // 如果音频在选择的视频关键帧之前稍微开始，这种情况可能会发生。
                // 钳制到0或根据需要处理。为简单起见，让我们跳过这些初始音频样本。
                Log.w(TAG, "跳过轨道 $sourceTrackIndex 的负调整时间戳样本: 原始=$presentationTimeUs, 实际开始=$actualStartTimeUs, 调整后=$adjustedTimeUs")
                extractor.advance()
                continue
            }

            bufferInfo.set(
                0,
                sampleSize,
                adjustedTimeUs, // 使用调整后的时间戳
                sampleFlags
            )

            // 写入前记录详细信息
            Log.d(TAG, "为轨道 $muxerTrackIndex 写入样本: 帧=$frameCount, 大小=$sampleSize, timeUs=$adjustedTimeUs, flags=${bufferInfo.flags}")

            muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
            samplesWritten = true // 一旦写入样本就设置标志
            frameCount++

            if (frameCount == 1 && isVideo) { // 检查*第一个写入*样本的标志
                firstWrittenSampleIsKey = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
                Log.d(TAG, "第一个写入的视频样本关键帧状态: $firstWrittenSampleIsKey")
            }

            extractor.advance()
        }
        Log.d(TAG, "完成处理轨道 $sourceTrackIndex。总共写入帧数: $frameCount。第一个写入的视频关键帧: $firstWrittenSampleIsKey")
    }
    
    /**
     * 尝试不同的seek策略来找到有效的视频帧
     * 
     * @param extractor MediaExtractor实例
     * @param startTimeUs 目标开始时间（微秒）
     * @param sourceTrackIndex 轨道索引
     * @param isVideo 是否为视频轨道
     * @return 是否成功找到有效帧
     */
    private fun trySeekStrategies(extractor: MediaExtractor, startTimeUs: Long, sourceTrackIndex: Int, isVideo: Boolean): Boolean {
        // 策略1: 尝试SEEK_TO_NEXT_SYNC
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_NEXT_SYNC)
        var sampleTime = extractor.sampleTime
        var sampleFlags = extractor.sampleFlags
        Log.d(TAG, "策略1: SEEK_TO_NEXT_SYNC 在 $startTimeUs 处用于轨道 $sourceTrackIndex。结果: time=${sampleTime}, flags=${sampleFlags}")
        
        if (sampleTime >= 0) {
            Log.d(TAG, "成功在请求的开始时间之后找到关键帧")
            return true
        }
        
        // 策略2: 尝试SEEK_TO_PREVIOUS_SYNC
        extractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        sampleTime = extractor.sampleTime
        sampleFlags = extractor.sampleFlags
        Log.d(TAG, "策略2: SEEK_TO_PREVIOUS_SYNC 在 $startTimeUs 处用于轨道 $sourceTrackIndex。结果: time=${sampleTime}, flags=${sampleFlags}")
        
        if (sampleTime >= 0) {
            Log.d(TAG, "成功在请求的开始时间之前找到关键帧")
            return true
        }
        
        // 策略3: 从文件开头开始，寻找第一个关键帧
        if (isVideo) {
            extractor.seekTo(0, MediaExtractor.SEEK_TO_NEXT_SYNC)
            sampleTime = extractor.sampleTime
            sampleFlags = extractor.sampleFlags
            val isKeyFrame = (sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0
            Log.d(TAG, "策略3: 为轨道 $sourceTrackIndex 寻找文件开头。结果: time=${sampleTime}, flags=${sampleFlags}, isKeyFrame=${isKeyFrame}")
            
            if (sampleTime >= 0) {
                Log.d(TAG, "在文件开头找到有效帧。将向前扫描以找到适当的帧。")
                return true
            }
        }
        
        // 所有策略都失败
        Log.e(TAG, "轨道 $sourceTrackIndex 的所有寻找策略都失败")
        return false
    }
}