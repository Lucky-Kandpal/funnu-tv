package com.saadho.funnutv.cache

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent video cache manager that adapts to system capacity
 * and prevents duplicate caching
 */
@UnstableApi
object VideoCacheManager {
    
    private var cache: Cache? = null
    private var systemCapacity: SystemCapacityAnalyzer.SystemCapacity? = null
    private val cachedVideos = ConcurrentHashMap<String, VideoCacheInfo>()
    private val preloadQueue = ConcurrentHashMap<String, Boolean>()
    
    data class VideoCacheInfo(
        val url: String,
        val cachedAt: Long,
        val size: Long,
        val isFullyCached: Boolean,
        val lastAccessed: Long
    )
    
    fun initialize(context: Context) {
        if (cache == null) {
            systemCapacity = SystemCapacityAnalyzer.analyzeSystemCapacity(context)
            val cacheSize = systemCapacity?.recommendedCacheSizeMB ?: 200L
            val cacheSizeBytes = cacheSize * 1024 * 1024
            
            val cacheDir = File(context.cacheDir, "video_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(cacheSizeBytes)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
            
            android.util.Log.d("VideoCacheManager", 
                "Initialized cache: ${cacheSize}MB, Strategy: ${systemCapacity?.preloadStrategy}")
        }
    }
    
    fun getCache(): Cache? = cache
    
    fun getSystemCapacity(): SystemCapacityAnalyzer.SystemCapacity? = systemCapacity
    
    /**
     * Check if video is already cached
     */
    fun isVideoCached(videoUrl: String): Boolean {
        return cachedVideos.containsKey(videoUrl) && 
               cachedVideos[videoUrl]?.isFullyCached == true
    }
    
    /**
     * Check if video is currently being cached
     */
    fun isVideoBeingCached(videoUrl: String): Boolean {
        return preloadQueue.containsKey(videoUrl)
    }
    
    /**
     * Mark video as being cached
     */
    fun markVideoAsCaching(videoUrl: String) {
        preloadQueue[videoUrl] = true
    }
    
    /**
     * Mark video as cached
     */
    fun markVideoAsCached(videoUrl: String, size: Long) {
        preloadQueue.remove(videoUrl)
        cachedVideos[videoUrl] = VideoCacheInfo(
            url = videoUrl,
            cachedAt = System.currentTimeMillis(),
            size = size,
            isFullyCached = true,
            lastAccessed = System.currentTimeMillis()
        )
        
        android.util.Log.d("VideoCacheManager", "Video cached: $videoUrl (${size / 1024}KB)")
    }
    
    /**
     * Update video access time
     */
    fun updateVideoAccess(videoUrl: String) {
        cachedVideos[videoUrl]?.let { info ->
            cachedVideos[videoUrl] = info.copy(lastAccessed = System.currentTimeMillis())
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): String {
        val cache = cache ?: return "Cache not initialized"
        val totalCached = cachedVideos.size
        val totalSize = cachedVideos.values.sumOf { it.size }
        val cacheSize = cache.cacheSpace
        
        return "Cached videos: $totalCached, " +
               "Total size: ${totalSize / (1024 * 1024)}MB, " +
               "Cache space: ${cacheSize / (1024 * 1024)}MB"
    }
    
    /**
     * Get videos that should be preloaded based on system capacity
     */
    fun getPreloadVideos(videoUrls: List<String>, currentIndex: Int): List<String> {
        val strategy = systemCapacity?.preloadStrategy ?: SystemCapacityAnalyzer.PreloadStrategy.CONSERVATIVE
        val maxConcurrent = systemCapacity?.maxConcurrentVideos ?: 2
        
        val preloadCount = when (strategy) {
            SystemCapacityAnalyzer.PreloadStrategy.AGGRESSIVE -> 5
            SystemCapacityAnalyzer.PreloadStrategy.MODERATE -> 3
            SystemCapacityAnalyzer.PreloadStrategy.CONSERVATIVE -> 2
            SystemCapacityAnalyzer.PreloadStrategy.MINIMAL -> 1
        }
        
        val videosToPreload = mutableListOf<String>()
        var preloadedCount = 0
        
        // Get next videos that aren't already cached or being cached
        for (i in 1..preloadCount) {
            val nextIndex = (currentIndex + i) % videoUrls.size
            val videoUrl = videoUrls[nextIndex]
            
            if (!isVideoCached(videoUrl) && !isVideoBeingCached(videoUrl) && preloadedCount < maxConcurrent) {
                videosToPreload.add(videoUrl)
                preloadedCount++
            }
        }
        
        return videosToPreload
    }
    
    /**
     * Clean up old cached videos based on LRU
     */
    fun cleanupOldVideos() {
        val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days
        val currentTime = System.currentTimeMillis()
        
        val iterator = cachedVideos.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val videoInfo = entry.value
            
            if (currentTime - videoInfo.lastAccessed > maxAge) {
                iterator.remove()
                android.util.Log.d("VideoCacheManager", "Removed old cached video: ${videoInfo.url}")
            }
        }
    }
    
    /**
     * Clear all cache
     */
    fun clearCache() {
        cache?.release()
        cache = null
        cachedVideos.clear()
        preloadQueue.clear()
    }
    
    /**
     * Get cache efficiency metrics
     */
    fun getCacheEfficiency(): String {
        val totalVideos = cachedVideos.size
        val fullyCached = cachedVideos.values.count { it.isFullyCached }
        val efficiency = if (totalVideos > 0) (fullyCached * 100) / totalVideos else 0
        
        return "Cache efficiency: $efficiency% ($fullyCached/$totalVideos videos fully cached)"
    }
}
