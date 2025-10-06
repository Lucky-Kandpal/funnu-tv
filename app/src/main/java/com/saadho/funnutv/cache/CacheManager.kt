package com.saadho.funnutv.cache

/**
 * Manages ExoPlayer cache with adaptive sizing based on available storage.
 * Cache size is 10% of available internal storage, capped at 500MB.
 */
import android.content.Context
import android.os.StatFs
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object CacheManager {

    private const val CACHE_PERCENTAGE = 0.1 // 10%
    private const val MAX_CACHE_SIZE = 500L * 1024 * 1024 // 500MB
    private const val MIN_CACHE_SIZE = 50L * 1024 * 1024 // 50MB minimum

    private var cache: Cache? = null

    fun initialize(context: Context) {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "exoplayer_cache")
            val cacheSize = calculateCacheSize(context)
            val evictor = LeastRecentlyUsedCacheEvictor(cacheSize)

            // Create a DatabaseProvider instance
            val databaseProvider = StandaloneDatabaseProvider(context)

            // Use new constructor with databaseProvider
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
    }

    fun getCache(): Cache? = cache

    private fun calculateCacheSize(context: Context): Long {
        val statFs = StatFs(context.filesDir.absolutePath)
        val availableBytes = statFs.availableBytes.toLong()
        val calculatedSize = (availableBytes * CACHE_PERCENTAGE).toLong()

        return when {
            calculatedSize > MAX_CACHE_SIZE -> MAX_CACHE_SIZE
            calculatedSize < MIN_CACHE_SIZE -> MIN_CACHE_SIZE
            else -> calculatedSize
        }
    }

    fun clearCache() {
        cache?.release()
        cache = null
    }

    fun getCacheStats(): String {
        val cache = cache ?: return "Cache not initialized"
        return "Cache size: ${cache.cacheSpace / (1024 * 1024)}MB"
    }
}
