package com.saadho.funnutv.player

import com.saadho.funnutv.cache.CacheManager
import com.saadho.funnutv.cache.VideoCacheManager
import com.saadho.funnutv.cache.VideoPreloader

/**
 * Singleton pool for managing ExoPlayer instances.
 * Maintains one active player and preloads the next video for seamless playback.
 */
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.saadho.funnutv.NetworkUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ExoPlayerPool {

    private var exoPlayer: ExoPlayer? = null
    private var currentVideoUrl: String? = null
    private val preloadQueue = mutableListOf<String>()
    private const val maxPreloadCount = 3
    private var context: Context? = null

    @OptIn(UnstableApi::class)
    fun initialize(context: Context) {
        this.context = context
        if (exoPlayer == null) {
            // Initialize intelligent caching system
            CacheManager.initialize(context)
            VideoPreloader.initialize(context)

            val dataSourceFactory = DefaultDataSource.Factory(context)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(CacheManager.getCache()!!)  // force non-null
                .setUpstreamDataSourceFactory(dataSourceFactory)
                .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

            val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(
                    androidx.media3.exoplayer.DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            15000, // Min buffer duration (15 seconds)
                            50000, // Max buffer duration (50 seconds)
                            2500,  // Buffer for playback (2.5 seconds)
                            5000   // Buffer for playback after rebuffer (5 seconds)
                        )
                        .setTargetBufferBytes(-1) // Use time-based buffering
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build()
                )
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = false
                    // Enable faster startup
                    setVideoScalingMode(androidx.media3.common.C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }
        }
    }

    fun getPlayer(): ExoPlayer? = exoPlayer
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    fun playVideo(videoUrl: String) {
        val player = exoPlayer ?: return
        val context = this.context ?: return

        try {
            // Check network connectivity before playing video
            if (!NetworkUtils.isNetworkAvailable(context)) {
                android.util.Log.w("ExoPlayerPool", "No network available, cannot play video: $videoUrl")
                return
            }
            
            if (currentVideoUrl != videoUrl) {
                // Update video access in cache manager
                VideoCacheManager.updateVideoAccess(videoUrl)
                
                // Prepare video on background thread for better performance
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val mediaItem = MediaItem.fromUri(videoUrl)
                        
                        // Switch back to main thread for player operations
                        withContext(Dispatchers.Main) {
                            player.setMediaItem(mediaItem, /* resetPosition = */ true)
                            player.prepare()
                            player.playWhenReady = true
                            currentVideoUrl = videoUrl
                            
                            val cacheStatus = if (VideoCacheManager.isVideoCached(videoUrl)) "cached" else "streaming"
                            android.util.Log.d("ExoPlayerPool", "Playing video: $videoUrl ($cacheStatus)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ExoPlayerPool", "Error preparing video: $videoUrl, error: ${e.message}")
                    }
                }
            } else {
                player.playWhenReady = true
                android.util.Log.d("ExoPlayerPool", "Resuming video: $videoUrl")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerPool", "Error playing video: $videoUrl, error: ${e.message}")
        }
    }


    fun preloadVideos(videoUrls: List<String>, currentIndex: Int = 0) {
        // Use the intelligent preloader
        VideoPreloader.preloadVideos(videoUrls, currentIndex)
    }

    fun pauseVideo() {
        exoPlayer?.playWhenReady = false
    }

    fun resumeVideo() {
        exoPlayer?.playWhenReady = true
    }

    fun stopVideo() {
        exoPlayer?.apply {
            stop()
            clearMediaItems()
        }
        currentVideoUrl = null
        preloadQueue.clear()
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying == true

    fun getCurrentPosition(): Long = exoPlayer?.currentPosition ?: 0L

    fun getDuration(): Long = exoPlayer?.duration ?: 0L

    fun release() {
        // Clean up intelligent preloader
        VideoPreloader.release()
        
        exoPlayer?.release()
        exoPlayer = null
        currentVideoUrl = null
        preloadQueue.clear()
    }

    fun addListener(listener: Player.Listener) {
        exoPlayer?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        exoPlayer?.removeListener(listener)
    }
}

