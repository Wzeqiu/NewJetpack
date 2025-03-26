package com.wzeqiu.mediacode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder


class VideoDecoder : SurfaceHolder.Callback {
    private val codec by lazy { MediaCodec.createDecoderByType("video/avc") }
    private val extractor by lazy { MediaExtractor() }
    private lateinit var surface: Surface
    private val handlerThread = HandlerThread("decode").apply { start() }
    private val handler by lazy { Handler(handlerThread.looper) }

    fun selectTrack(path: String) {

        Log.e("AAAAA", "path=====$path")
        extractor.setDataSource(path)
        // 选择视频轨道
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            if (format.getString(MediaFormat.KEY_MIME)!!.startsWith("video/")) {
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

    fun setDataSource(path: String) {

        selectTrack(path)
    }

    fun start() {
        Thread {


            val format = MediaFormat.createVideoFormat("video/avc", 200, 200)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 4000000)
            codec.configure(format, surface, null, 0)

            codec.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    if (index >= 0) {
                        val inputBuffer = codec.getInputBuffer(index)
                        val sampleSize = extractor.readSampleData(inputBuffer!!, 0)
                        if (sampleSize >= 0) {
                            codec.queueInputBuffer(index, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        } else {
                            // 发送结束标志
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        }
                    }
                }

                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    codec.releaseOutputBuffer(index, true)
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                }
            }, handler)
            codec.start()
        }.start()
    }

}