package com.common.utils

import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import java.io.File

object PathManager {
    private val CACHE_PATH = Utils.getApp().cacheDir.absolutePath
    val CACHE_IMAGE = CACHE_PATH + File.separator + "image"



    fun init() {
        FileUtils.createOrExistsDir(CACHE_IMAGE)
    }
}