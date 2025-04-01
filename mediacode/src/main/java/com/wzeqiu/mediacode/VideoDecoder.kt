package com.wzeqiu.mediacode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder


class VideoDecoder : SurfaceHolder.Callback {
    private var codec: MediaCodec? = null
    private val extractor by lazy { MediaExtractor() }
    private lateinit var surface: Surface
    private val handlerThread by lazy { HandlerThread("decode").apply { start() } }
    private val handler by lazy { Handler(handlerThread.looper) }
    private var selectVideoTrack = -1

    fun selectTrack(path: String) {

        Log.e("AAAAA", "path=====$path")
        extractor.setDataSource(path)
        // 选择视频轨道
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)!!.startsWith("video/")) {
                selectVideoTrack = i
                extractor.selectTrack(i)
                break
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.e("AAAAAA", "surfaceCreated")
        surface = holder.surface


    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.e("AAAAAA", "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.e("AAAAAA", "surfaceDestroyed")
//        codec.stop()
//        codec.release()
//        extractor.release()
    }

    var width = 0
    var height = 0
    fun setDataSource(path: String) {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(path)
        val widthStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        mmr.release()
        width = widthStr!!.toInt()
        height = heightStr!!.toInt()
        selectTrack(path)
    }

    fun start() {
        Thread {


            val format = extractor.getTrackFormat(selectVideoTrack)

            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec!!.configure(format, surface, null, 0)

            codec!!.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    if (index >= 0) {
                        val inputBuffer = codec.getInputBuffer(index)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                        if (sampleSize >= 0) {
                            codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        } else {
                            // 发送结束标志
                            codec.queueInputBuffer(
                                index,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                        }
                    }
                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    index: Int,
                    info: MediaCodec.BufferInfo
                ) {
                    codec.releaseOutputBuffer(index, true)
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                }
            }, handler)
            codec!!.start()
        }.start()
    }

}