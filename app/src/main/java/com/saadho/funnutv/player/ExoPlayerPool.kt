package com.saadho.funnutv.player

import com.saadho.funnutv.cache.CacheManager

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
            CacheManager.initialize(context)

            val dataSourceFactory = DefaultDataSource.Factory(context)
            val cacheDataSourceFactory = CacheDataSource.Factory()
                .setCache(CacheManager.getCache()!!)  // force non-null
                .setUpstreamDataSourceFactory(dataSourceFactory)

            val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = false
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
                val mediaItem = MediaItem.fromUri(videoUrl)
                player.setMediaItem(mediaItem, /* resetPosition = */ true)  // example usage
                player.prepare()
                player.playWhenReady = true
                currentVideoUrl = videoUrl
                android.util.Log.d("ExoPlayerPool", "Playing video: $videoUrl")
            } else {
                player.playWhenReady = true
                android.util.Log.d("ExoPlayerPool", "Resuming video: $videoUrl")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExoPlayerPool", "Error playing video: $videoUrl, error: ${e.message}")
        }
    }


    fun preloadVideos(videoUrls: List<String>) {
        val player = exoPlayer ?: return
        val context = this.context ?: return
        
        // Check network connectivity before preloading videos
        if (!NetworkUtils.isNetworkAvailable(context)) {
            android.util.Log.w("ExoPlayerPool", "No network available, skipping video preloading")
            return
        }
        
        preloadQueue.clear()
        videoUrls.take(maxPreloadCount).forEach { url ->
            if (url != currentVideoUrl) {
                preloadQueue.add(url)
            }
        }
        preloadQueue.forEachIndexed { index, url ->
            val mediaItem = MediaItem.fromUri(url)
            if (player.mediaItemCount <= index) {
                player.addMediaItem(mediaItem)
            } else {
                // Remove the old media item at index, then add the new one at the same index
                player.removeMediaItem(index)
                player.addMediaItem(index, mediaItem)
            }
        }

        android.util.Log.d("ExoPlayerPool", "Preloaded ${preloadQueue.size} videos")
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

