package com.saadho.funnutv.cache

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import java.io.File

/**
 * Analyzes system capacity to determine optimal caching strategies
 */
object SystemCapacityAnalyzer {
    
    data class SystemCapacity(
        val totalStorageGB: Float,
        val availableStorageGB: Float,
        val totalMemoryMB: Long,
        val availableMemoryMB: Long,
        val isLowEndDevice: Boolean,
        val recommendedCacheSizeMB: Long,
        val maxConcurrentVideos: Int,
        val preloadStrategy: PreloadStrategy
    )
    
    enum class PreloadStrategy {
        AGGRESSIVE,    // High-end devices: preload 3-5 videos
        MODERATE,      // Mid-range devices: preload 2-3 videos  
        CONSERVATIVE,  // Low-end devices: preload 1-2 videos
        MINIMAL        // Very low-end devices: preload 0-1 videos
    }
    
    fun analyzeSystemCapacity(context: Context): SystemCapacity {
        val storageInfo = getStorageInfo(context)
        val memoryInfo = getMemoryInfo(context)
        val isLowEndDevice = isLowEndDevice(memoryInfo, storageInfo)
        
        val recommendedCacheSize = calculateRecommendedCacheSize(storageInfo, memoryInfo, isLowEndDevice)
        val maxConcurrentVideos = calculateMaxConcurrentVideos(memoryInfo, isLowEndDevice)
        val preloadStrategy = determinePreloadStrategy(storageInfo, memoryInfo, isLowEndDevice)
        
        return SystemCapacity(
            totalStorageGB = storageInfo.totalGB,
            availableStorageGB = storageInfo.availableGB,
            totalMemoryMB = memoryInfo.totalMB,
            availableMemoryMB = memoryInfo.availableMB,
            isLowEndDevice = isLowEndDevice,
            recommendedCacheSizeMB = recommendedCacheSize,
            maxConcurrentVideos = maxConcurrentVideos,
            preloadStrategy = preloadStrategy
        )
    }
    
    private data class StorageInfo(
        val totalGB: Float,
        val availableGB: Float,
        val usedPercentage: Float
    )
    
    private data class MemoryInfo(
        val totalMB: Long,
        val availableMB: Long,
        val usedPercentage: Float
    )
    
    private fun getStorageInfo(context: Context): StorageInfo {
        val internalStorage = File(context.filesDir.absolutePath)
        val statFs = StatFs(internalStorage.absolutePath)
        
        val totalBytes = statFs.blockCountLong * statFs.blockSizeLong
        val availableBytes = statFs.availableBytes
        
        val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val availableGB = availableBytes / (1024.0 * 1024.0 * 1024.0)
        val usedPercentage = ((totalBytes - availableBytes).toFloat() / totalBytes.toFloat()) * 100
        
        return StorageInfo(totalGB.toFloat(), availableGB.toFloat(), usedPercentage)
    }
    
    private fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMB = memoryInfo.totalMem / (1024 * 1024)
        val availableMB = memoryInfo.availMem / (1024 * 1024)
        val usedPercentage = ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem.toFloat()) * 100
        
        return MemoryInfo(totalMB, availableMB, usedPercentage)
    }
    
    private fun isLowEndDevice(memoryInfo: MemoryInfo, storageInfo: StorageInfo): Boolean {
        return memoryInfo.totalMB < 2048 || // Less than 2GB RAM
               storageInfo.totalGB < 16 ||  // Less than 16GB storage
               memoryInfo.usedPercentage > 80 || // More than 80% memory used
               storageInfo.usedPercentage > 85   // More than 85% storage used
    }
    
    private fun calculateRecommendedCacheSize(
        storageInfo: StorageInfo, 
        memoryInfo: MemoryInfo, 
        isLowEndDevice: Boolean
    ): Long {
        val availableStorageMB = (storageInfo.availableGB * 1024).toLong()
        
        return when {
            isLowEndDevice -> {
                // Conservative caching for low-end devices
                minOf(availableStorageMB / 20, 200) // Max 200MB or 5% of available storage
            }
            memoryInfo.totalMB < 4096 -> {
                // Mid-range devices
                minOf(availableStorageMB / 15, 500) // Max 500MB or ~6.7% of available storage
            }
            else -> {
                // High-end devices
                minOf(availableStorageMB / 10, 1024) // Max 1GB or 10% of available storage
            }
        }
    }
    
    private fun calculateMaxConcurrentVideos(memoryInfo: MemoryInfo, isLowEndDevice: Boolean): Int {
        return when {
            isLowEndDevice -> 1
            memoryInfo.totalMB < 4096 -> 2
            memoryInfo.totalMB < 8192 -> 3
            else -> 4
        }
    }
    
    private fun determinePreloadStrategy(
        storageInfo: StorageInfo, 
        memoryInfo: MemoryInfo, 
        isLowEndDevice: Boolean
    ): PreloadStrategy {
        return when {
            isLowEndDevice -> PreloadStrategy.MINIMAL
            memoryInfo.totalMB < 4096 || storageInfo.availableGB < 8 -> PreloadStrategy.CONSERVATIVE
            memoryInfo.totalMB < 8192 || storageInfo.availableGB < 16 -> PreloadStrategy.MODERATE
            else -> PreloadStrategy.AGGRESSIVE
        }
    }
}
