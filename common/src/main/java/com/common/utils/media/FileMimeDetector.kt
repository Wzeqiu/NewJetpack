package com.common.utils.media

import java.io.File
import java.io.FileInputStream

object FileMimeDetector {

    private const val HEAD_BYTES_LENGTH = 16

    /**
     * 获取文件的MIME类型
     * @return 具体的MIME类型字符串，如"image/jpeg"，未知类型返回null
     */
    fun File?.getMimeType(): String? {
        return this?.takeIf { it.exists() && it.isFile }
            ?.readHeadBytes()
            ?.let { bytes ->
                detectImageMimeType(bytes) ?: detectVideoMimeType(bytes)
            }
    }

    /**
     * 判断文件是否为图片
     */
    fun File?.isImage(): Boolean {
        return getMimeType()?.startsWith("image/") == true
    }

    /**
     * 判断文件是否为视频
     */
    fun File?.isVideo(): Boolean {
        return getMimeType()?.startsWith("video/") == true
    }

    /**
     * 读取文件头部的指定字节数
     */
    private fun File.readHeadBytes(length: Int = HEAD_BYTES_LENGTH): ByteArray? = runCatching {
        FileInputStream(this).use { inputStream ->
            val bytes = ByteArray(length)
            val read = inputStream.read(bytes, 0, length)
            bytes.takeIf { read > 0 }
        }
    }.getOrDefault(null)

    /**
     * 检测图片类型并返回对应的MIME类型
     */
    private fun detectImageMimeType(bytes: ByteArray): String? {
        return when {
            isJpeg(bytes) -> "image/jpeg"
            isPng(bytes) -> "image/png"
            isGif(bytes) -> "image/gif"
            isBmp(bytes) -> "image/bmp"
            isWebp(bytes) -> "image/webp"
            isHeif(bytes) -> if (bytes[11] == 0x63.toByte()) "image/heic" else "image/heif"
            else -> null
        }
    }

    /**
     * 检测视频类型并返回对应的MIME类型
     */
    private fun detectVideoMimeType(bytes: ByteArray): String? {
        return when {
            isMp4(bytes) -> "video/mp4"
            isMkv(bytes) -> "video/x-matroska"
            isAvi(bytes) -> "video/x-msvideo"
            isFlv(bytes) -> "video/x-flv"
            is3gp(bytes) -> "video/3gpp"
            isMpeg(bytes) -> if (bytes[3] == 0xBA.toByte()) "video/mpeg" else "video/mpg"
            else -> null
        }
    }

    // 图片类型检测方法
    private fun isJpeg(bytes: ByteArray) = bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()

    private fun isPng(bytes: ByteArray) = bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() && bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() && bytes[4] == 0x0D.toByte() && bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() && bytes[7] == 0x0A.toByte()

    private fun isGif(bytes: ByteArray) = bytes.size >= 4 &&
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x38.toByte()

    private fun isBmp(bytes: ByteArray) = bytes.size >= 2 &&
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte()

    private fun isWebp(bytes: ByteArray) = bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() && bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()

    private fun isHeif(bytes: ByteArray) = bytes.size >= 12 &&
            bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() && bytes[6] == 0x79.toByte() &&
            bytes[7] == 0x70.toByte() && bytes[8] == 0x68.toByte() && bytes[9] == 0x65.toByte() &&
            bytes[10] == 0x69.toByte() && bytes[11] in setOf(0x63.toByte(), 0x66.toByte())

    // 视频类型检测方法
    private fun isMp4(bytes: ByteArray) = bytes.size >= 12 &&
            bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() && bytes[6] == 0x79.toByte() &&
            bytes[7] == 0x70.toByte() && (
            (bytes[8] == 0x6D.toByte() && bytes[9] == 0x70.toByte() &&
                    bytes[10] == 0x34.toByte() && bytes[11] == 0x32.toByte()) ||
                    (bytes[8] == 0x69.toByte() && bytes[9] == 0x73.toByte() &&
                            bytes[10] == 0x6F.toByte() && bytes[11] == 0x6D.toByte())
            )

    private fun isMkv(bytes: ByteArray) = bytes.size >= 4 &&
            bytes[0] == 0x1A.toByte() && bytes[1] == 0x45.toByte() &&
            bytes[2] == 0xDF.toByte() && bytes[3] == 0xA3.toByte()

    private fun isAvi(bytes: ByteArray) = bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() && bytes[2] == 0x46.toByte() &&
            bytes[3] == 0x46.toByte() && bytes[8] == 0x41.toByte() && bytes[9] == 0x56.toByte() &&
            bytes[10] == 0x49.toByte() && bytes[11] == 0x20.toByte()

    private fun isFlv(bytes: ByteArray) = bytes.size >= 4 &&
            bytes[0] == 0x46.toByte() && bytes[1] == 0x4C.toByte() &&
            bytes[2] == 0x56.toByte() && bytes[3] == 0x01.toByte()

    private fun is3gp(bytes: ByteArray) = bytes.size >= 12 &&
            bytes[4] == 0x66.toByte() && bytes[5] == 0x74.toByte() && bytes[6] == 0x79.toByte() &&
            bytes[7] == 0x70.toByte() && bytes[8] == 0x33.toByte() && bytes[9] == 0x67.toByte() &&
            bytes[10] == 0x70.toByte()

    private fun isMpeg(bytes: ByteArray) = bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
            bytes[2] == 0x01.toByte() && bytes[3] in setOf(0xBA.toByte(), 0xB3.toByte())
}
