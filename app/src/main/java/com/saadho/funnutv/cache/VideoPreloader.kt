package com.saadho.funnutv.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.saadho.funnutv.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Intelligent video preloader that adapts to system capacity
 * and prevents duplicate preloading
 */
@OptIn(UnstableApi::class)
object VideoPreloader {
    
    private var preloadPlayer: ExoPlayer? = null
    private var context: Context? = null
    private var preloadJobs = mutableMapOf<String, Job>()
    private var systemCapacity: SystemCapacityAnalyzer.SystemCapacity? = null
    
    fun initialize(context: Context) {
        this.context = context
        this.systemCapacity = VideoCacheManager.getSystemCapacity()
        
        if (preloadPlayer == null) {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(VideoCacheManager.getCache()!!)
                .setUpstreamDataSourceFactory(dataSourceFactory)
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

            preloadPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    playWhenReady = false
                }
        }
    }
    
    /**
     * Preload videos intelligently based on system capacity
     */
    fun preloadVideos(videoUrls: List<String>, currentIndex: Int) {
        val context = this.context ?: return
        val systemCapacity = this.systemCapacity ?: return
        
        // Check network connectivity
        if (!NetworkUtils.isNetworkAvailable(context)) {
            android.util.Log.w("VideoPreloader", "No network available, skipping preload")
            return
        }
        
        // Get videos that should be preloaded
        val videosToPreload = VideoCacheManager.getPreloadVideos(videoUrls, currentIndex)
        
        android.util.Log.d("VideoPreloader", 
            "Preloading ${videosToPreload.size} videos (strategy: ${systemCapacity.preloadStrategy})")
        
        // Preload each video
        videosToPreload.forEach { videoUrl ->
            preloadVideo(videoUrl)
        }
    }
    
    /**
     * Preload a single video
     */
    private fun preloadVideo(videoUrl: String) {
        val player = preloadPlayer ?: return
        
        // Skip if already cached or being cached
        if (VideoCacheManager.isVideoCached(videoUrl) || 
            VideoCacheManager.isVideoBeingCached(videoUrl)) {
            android.util.Log.d("VideoPreloader", "Skipping already cached video: $videoUrl")
            return
        }
        
        // Mark as being cached
        VideoCacheManager.markVideoAsCaching(videoUrl)
        
        // Cancel any existing preload job for this video
        preloadJobs[videoUrl]?.cancel()
        
        // Start preload job
        val job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val mediaItem = MediaItem.fromUri(videoUrl)
                
                withContext(Dispatchers.Main) {
                    // Add to player for preloading
                    player.addMediaItem(mediaItem)
                    player.prepare()
                    
                    // Don't start playback, just prepare
                    android.util.Log.d("VideoPreloader", "Preloading video: $videoUrl")
                }
                
                // Simulate cache completion (in real implementation, you'd track actual cache progress)
                withContext(Dispatchers.Main) {
                    // Estimate cache size (this would be more accurate in real implementation)
                    val estimatedSize = 50 * 1024 * 1024L // 50MB estimate
                    VideoCacheManager.markVideoAsCached(videoUrl, estimatedSize)
                    preloadJobs.remove(videoUrl)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("VideoPreloader", "Error preloading video: $videoUrl, error: ${e.message}")
                VideoCacheManager.markVideoAsCaching(videoUrl) // Remove from being cached
                preloadJobs.remove(videoUrl)
            }
        }
        
        preloadJobs[videoUrl] = job
    }
    
    /**
     * Cancel preloading for specific video
     */
    fun cancelPreload(videoUrl: String) {
        preloadJobs[videoUrl]?.cancel()
        preloadJobs.remove(videoUrl)
    }
    
    /**
     * Cancel all preloading
     */
    fun cancelAllPreloads() {
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
    }
    
    /**
     * Get preload statistics
     */
    fun getPreloadStats(): String {
        val activeJobs = preloadJobs.size
        val cachedVideos = VideoCacheManager.getCacheStats()
        return "Active preloads: $activeJobs, $cachedVideos"
    }
    
    /**
     * Cleanup resources
     */
    fun release() {
        cancelAllPreloads()
        preloadPlayer?.release()
        preloadPlayer = null
        context = null
        systemCapacity = null
    }
}
