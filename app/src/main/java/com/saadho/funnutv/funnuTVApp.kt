package com.saadho.funnutv

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.google.firebase.FirebaseApp
import com.saadho.funnutv.cache.CacheManager
import com.saadho.funnutv.player.ExoPlayerPool

class funnuTVApp : Application() {
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                // Initialize Firebase
                FirebaseApp.initializeApp(this)
                android.util.Log.d("funnuTVApp", "Firebase initialized successfully")
            } else {
                android.util.Log.d("funnuTVApp", "Firebase already initialized")
            }
        } catch (e: Exception) {
            android.util.Log.e("funnuTVApp", "Failed to initialize Firebase: ${e.message}", e)
            // Continue app initialization even if Firebase fails
        }

        // Initialize cache and player pool
        CacheManager.initialize(this)
        ExoPlayerPool.initialize(this)
    }
}