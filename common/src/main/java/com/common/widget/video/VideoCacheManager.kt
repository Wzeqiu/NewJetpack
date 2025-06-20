package com.common.widget.video

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * 视频缓存管理器
 * 负责Media3的视频缓存功能
 */
object VideoCacheManager {
    // 默认缓存大小：500MB
    private const val DEFAULT_MAX_CACHE_SIZE: Long = 500 * 1024 * 1024
    
    private var cache: Cache? = null
    
    /**
     * 获取缓存实例
     */
    @Synchronized
    fun getCache(context: Context): Cache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val evictor = LeastRecentlyUsedCacheEvictor(DEFAULT_MAX_CACHE_SIZE)
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }
    
    /**
     * 创建支持缓存的数据源工厂
     */
    fun buildCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        
        // 上游数据源工厂（处理HTTP请求）
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
        
        // 创建默认数据源工厂（处理所有协议）
        val upstreamFactory = DefaultDataSource.Factory(
            context,
            httpDataSourceFactory
        )
        
        // 创建缓存数据源工厂
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // 允许写入缓存
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setFileDataSourceFactory(FileDataSource.Factory())
    }
    
    /**
     * 清除所有缓存
     */
    fun clearCache() {
        cache?.let {
            try {
                it.release()
                cache = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取缓存大小（字节）
     */
    fun getCacheSize(): Long {
        return cache?.cacheSpace ?: 0
    }
} 