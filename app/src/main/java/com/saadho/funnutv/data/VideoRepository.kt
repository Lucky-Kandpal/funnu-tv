package com.saadho.funnutv.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Repository for managing video data
 */
//class VideoRepository(private val context: Context) {
//
//    private val gson = GsonBuilder()
//        .setLenient()
//        .create()
//
//    /**
//     * Load videos from the JSON file in raw resources
//     */
//    suspend fun loadVideos(): Result<List<Video>> = withContext(Dispatchers.IO) {
//        try {
//            val resourceId = context.resources.getIdentifier("sample_videos", "raw", context.packageName)
//            if (resourceId == 0) {
//                return@withContext Result.failure(Exception("sample_videos.json not found in raw resources"))
//            }
//
//            val jsonString = context.resources.openRawResource(resourceId)
//                .bufferedReader()
//                .use { it.readText() }
//
//            if (jsonString.isBlank()) {
//                return@withContext Result.failure(Exception("Empty JSON file"))
//            }
//
//            // Parse JSON directly to VideoResponse
//            val videoResponse = gson.fromJson(jsonString, VideoResponse::class.java)
//            if (videoResponse.videos.isEmpty()) {
//                return@withContext Result.failure(Exception("No videos found in JSON"))
//            }
//
//            Result.success(videoResponse.videos)
//        } catch (e: IOException) {
//            Result.failure(Exception("Failed to read JSON file: ${e.message}", e))
//        } catch (e: Exception) {
//            Result.failure(Exception("Failed to parse JSON: ${e.message}", e))
//        }
//    }
//
//    /**
//     * Get a video by ID
//     */
//    suspend fun getVideoById(id: String): Result<Video?> = withContext(Dispatchers.IO) {
//        try {
//            val videosResult = loadVideos()
//            if (videosResult.isSuccess) {
//                val video = videosResult.getOrNull()?.find { it.id == id }
//                Result.success(video)
//            } else {
//                Result.failure(videosResult.exceptionOrNull() ?: Exception("Failed to load videos"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Get videos with pagination support for infinite scrolling
//     */
//    suspend fun getVideos(offset: Int = 0, limit: Int = 10): Result<List<Video>> = withContext(Dispatchers.IO) {
//        try {
//            val videosResult = loadVideos()
//            if (videosResult.isSuccess) {
//                val allVideos = videosResult.getOrNull() ?: emptyList()
//                val paginatedVideos = allVideos.drop(offset).take(limit)
//                Result.success(paginatedVideos)
//            } else {
//                Result.failure(videosResult.exceptionOrNull() ?: Exception("Failed to load videos"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    /**
//     * Get infinite videos for continuous scrolling
//     */
//    suspend fun getInfiniteVideos(startIndex: Int = 0, count: Int = 20): Result<List<Video>> = withContext(Dispatchers.IO) {
//        try {
//            val videosResult = loadVideos()
//            if (videosResult.isSuccess) {
//                val allVideos = videosResult.getOrNull() ?: emptyList()
//                if (allVideos.isEmpty()) {
//                    return@withContext Result.success(emptyList())
//                }
//
//                // Create infinite list by repeating the base videos
//                val infiniteVideos = mutableListOf<Video>()
//                for (i in 0 until count) {
//                    val videoIndex = (startIndex + i) % allVideos.size
//                    val originalVideo = allVideos[videoIndex]
//                    // Create a unique video with modified ID for infinite scrolling
//                    val infiniteVideo = originalVideo.copy(
//                        id = "${originalVideo.id}_${startIndex + i}",
//                        title = "${originalVideo.title} (${(startIndex + i) / allVideos.size + 1})"
//                    )
//                    infiniteVideos.add(infiniteVideo)
//                }
//                Result.success(infiniteVideos)
//            } else {
//                Result.failure(videosResult.exceptionOrNull() ?: Exception("Failed to load videos"))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}
//
//

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import com.saadho.funnutv.NetworkUtils

class VideoRepository(private val context: Context) {

    private val gson = GsonBuilder().setLenient().create()
    
    // Lazy initialization of Firestore to ensure Firebase is initialized
    private val firestore: FirebaseFirestore by lazy {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseFirestore.getInstance()
            } else {
                throw IllegalStateException("Firebase is not initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Failed to initialize Firestore: ${e.message}", e)
            throw IllegalStateException("Firebase is not initialized: ${e.message}")
        }
    }

    // Load from Firestore first, fallback to local JSON
    suspend fun loadVideos(): Result<List<Video>> = withContext(Dispatchers.IO) {
        android.util.Log.d("VideoRepository", "loadVideos() called")

        try {
            // Check network connectivity first
            if (!NetworkUtils.isNetworkAvailable(context)) {
                android.util.Log.w("VideoRepository", "No network available, falling back to local JSON")
                return@withContext loadLocalVideos()
            }
            
            // Check if Firebase is initialized
            val firebaseApps = FirebaseApp.getApps(context)
            android.util.Log.d("VideoRepository", "Firebase apps count: ${firebaseApps.size}")
            
            if (firebaseApps.isEmpty()) {
                android.util.Log.w("VideoRepository", "Firebase not initialized, falling back to local JSON")
                return@withContext loadLocalVideos()
            }
            
            // Additional check to ensure the default app is initialized
            try {
                val defaultApp = FirebaseApp.getInstance()
                android.util.Log.d("VideoRepository", "Firebase default app: ${defaultApp.name}")
            } catch (e: Exception) {
                android.util.Log.w("VideoRepository", "Firebase default app not available: ${e.message}, falling back to local JSON")
                return@withContext loadLocalVideos()
            }

            val snapshot = firestore.collection("videos").get().await()
            android.util.Log.d("VideoRepository", "Firestore snapshot size: ${snapshot.size()}")
            
            val videos = snapshot.toObjects(Video::class.java)
            android.util.Log.d("VideoRepository", "Deserialized ${videos.size} videos from Firestore")

            if (videos.isNotEmpty()) {
                android.util.Log.d("VideoRepository", "Loaded ${videos.size} videos from Firestore")
                return@withContext Result.success(videos)
            } else {
                android.util.Log.w("VideoRepository", "No videos in Firestore, falling back to local JSON")
                return@withContext loadLocalVideos()
            }

        } catch (e: SecurityException) {
            android.util.Log.w("VideoRepository", "Firestore access blocked by security policy: ${e.message}, falling back to local JSON")
            return@withContext loadLocalVideos()
        } catch (e: Exception) {
            android.util.Log.w("VideoRepository", "Firestore error: ${e.message}, falling back to local JSON", e)
            return@withContext loadLocalVideos()
        }
    }

    private fun loadLocalVideos(): Result<List<Video>> {
        android.util.Log.d("VideoRepository", "loadLocalVideos() called")

        return try {
            val resourceId = context.resources.getIdentifier("sample_videos", "raw", context.packageName)
            if (resourceId == 0) {
                android.util.Log.e("VideoRepository", "sample_videos.json not found in raw resources")
                return Result.failure(Exception("sample_videos.json not found"))
            }

            val jsonString = context.resources.openRawResource(resourceId)
                .bufferedReader().use { it.readText() }

            if (jsonString.isBlank()) {
                android.util.Log.e("VideoRepository", "Empty JSON file")
                return Result.failure(Exception("Empty JSON file"))
            }

            val videoResponse = gson.fromJson(jsonString, VideoResponse::class.java)
            if (videoResponse.videos.isEmpty()) {
                android.util.Log.e("VideoRepository", "No videos found in JSON")
                return Result.failure(Exception("No videos found"))
            }

            android.util.Log.d("VideoRepository", "Loaded ${videoResponse.videos.size} videos from local JSON")
            Result.success(videoResponse.videos)
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Error loading local videos: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getInfiniteVideos(startIndex: Int = 0, count: Int = 20): Result<List<Video>> = withContext(Dispatchers.IO) {
        android.util.Log.d(
            "VideoRepository",
            "getInfiniteVideos() called with startIndex=$startIndex, count=$count"
        )

        try {
            val videosResult = loadVideos()
            if (videosResult.isSuccess) {
                val allVideos = videosResult.getOrNull() ?: emptyList()
                android.util.Log.d("VideoRepository", "Total videos loaded: ${allVideos.size}")

                if (allVideos.isEmpty()) {
                    android.util.Log.w(
                        "VideoRepository",
                        "No videos available to repeat infinitely"
                    )
                    return@withContext Result.success(emptyList())
                }

                val infiniteVideos = mutableListOf<Video>()
                for (i in 0 until count) {
                    val videoIndex = (startIndex + i) % allVideos.size
                    val original = allVideos[videoIndex]
                    infiniteVideos.add(
                        original.copy(
                            id = "${original.id}_${startIndex + i}",
                            title = "${original.title} (${(startIndex + i) / allVideos.size + 1})"
                        )
                    )
                }
                android.util.Log.d(
                    "VideoRepository",
                    "Returning ${infiniteVideos.size} infinite videos"
                )
                Result.success(infiniteVideos)
            } else {
                val error = videosResult.exceptionOrNull() ?: Exception("Failed to load videos")
                android.util.Log.e(
                    "VideoRepository",
                    "Failed to load videos: ${error.message}",
                    error
                )
                Result.failure(error)
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoRepository", "Exception in getInfiniteVideos: ${e.message}", e)
            Result.failure(e)
        }
    }
}