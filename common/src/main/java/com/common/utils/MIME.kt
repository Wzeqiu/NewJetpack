package com.common.utils

private val mimeTypeMap: Map<String, String> = mapOf(
    // 图片类型
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "gif" to "image/gif",
    "bmp" to "image/bmp",
    "webp" to "image/webp",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "ico" to "image/x-icon",
    "tif" to "image/tiff",
    "tiff" to "image/tiff",
    "svg" to "image/svg+xml",
    "psd" to "image/vnd.adobe.photoshop",  // 额外补充

    // 视频类型
    "mp4" to "video/mp4",
    "mov" to "video/quicktime",
    "avi" to "video/x-msvideo",
    "mkv" to "video/x-matroska",
    "flv" to "video/x-flv",
    "webm" to "video/webm",
    "mpeg" to "video/mpeg",
    "mpg" to "video/mpeg",
    "3gp" to "video/3gpp",
    "3g2" to "video/3gpp2",
    "wmv" to "video/x-ms-wmv",
    "ts" to "video/mp2t",         // 补充流媒体格式
    "m2ts" to "video/mp2t",

    // 音频类型
    "mp3" to "audio/mpeg",
    "wav" to "audio/wav",
    "ogg" to "audio/ogg",
    "aac" to "audio/aac",
    "flac" to "audio/flac",
    "mid" to "audio/midi",
    "midi" to "audio/midi",
    "amr" to "audio/amr",
    "m4a" to "audio/mp4",
    "opus" to "audio/opus",
    "mka" to "audio/x-matroska",
    "aif" to "audio/x-aiff",      // 补充 AIFF 格式
    "aiff" to "audio/x-aiff",
    "ac3" to "audio/ac3",
    "wma" to "audio/x-ms-wma",
    "ra" to "audio/vnd.rn-realaudio"
)
// 使用示例
fun getMimeType(suffix: String): String {
    return mimeTypeMap[suffix] ?: "application/octet-stream"
}