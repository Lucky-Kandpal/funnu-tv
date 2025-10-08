package com.saadho.funnutv.cache

/**
 * Legacy cache manager - now delegates to the new intelligent caching system
 */
import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache

@UnstableApi
object CacheManager {

    fun initialize(context: Context) {
        // Initialize the new intelligent caching system
        VideoCacheManager.initialize(context)
    }

    fun getCache(): Cache? = VideoCacheManager.getCache()

    fun clearCache() {
        VideoCacheManager.clearCache()
    }

    fun getCacheStats(): String {
        return VideoCacheManager.getCacheStats()
    }
}
