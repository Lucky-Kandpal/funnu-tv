package com.saadho.funnutv.ui.activities

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.saadho.funnutv.ui.layout.VideoLayoutManager
import com.saadho.funnutv.ui.gesture.VideoGestureHandler
import com.saadho.funnutv.R
import com.saadho.funnutv.adapter.VideoAdapter
import com.saadho.funnutv.data.Video
import com.saadho.funnutv.player.ExoPlayerPool
import com.saadho.funnutv.viewmodel.VideoViewModel
import com.saadho.funnutv.NetworkUtils

import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import android.widget.ImageView
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.core.view.ViewCompat

/**
 * Main activity for Funnu TV - displays auto-playing video feed
 * Enforces landscape orientation and handles 4-direction scrolling
 */


class MainActivity : AppCompatActivity() {

    private lateinit var videoRecyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorMessage: TextView
    private lateinit var videoAdapter: VideoAdapter
    private lateinit var viewModel: VideoViewModel
    private lateinit var layoutManager: VideoLayoutManager
    private lateinit var gestureHandler: VideoGestureHandler

    // Network connectivity monitoring
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var isNetworkAvailable = true

    // No internet UI elements
    private lateinit var noInternetLayout: View
    private lateinit var retryButton: MaterialButton
    private lateinit var wifiIcon: ImageView

    // Splash screen audio
    private var splashMediaPlayer: MediaPlayer? = null
    private var splashScreen: androidx.core.splashscreen.SplashScreen? = null

    private fun applyEdgeToEdge() {
        // Ensures your layout draws behind system bars
        enableEdgeToEdge()

        // Disable fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Let system handle light/dark bar icons automatically
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun playSplashAudio() {
        try {
            splashMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(
                    this@MainActivity,
                    "android.resource://${packageName}/${R.raw.a_intro}".toUri()
                )
                prepare()
                start()

                setOnCompletionListener {
                    // Hide splash screen when audio finishes
                    splashScreen?.setOnExitAnimationListener { splashScreenView ->
                        splashScreenView.remove()
                    }
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("MainActivity", "MediaPlayer error: what=$what, extra=$extra")
                    // Hide splash screen immediately if audio fails
                    splashScreen?.setOnExitAnimationListener { splashScreenView ->
                        splashScreenView.remove()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error playing splash audio: ${e.message}")
            splashScreen?.setOnExitAnimationListener { splashScreenView ->
                splashScreenView.remove()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        applyEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        splashScreen = installSplashScreen()


        // Play splash audio
        playSplashAudio()

        // Enforce landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Set full screen
        setupFullScreen()
        
        setContentView(R.layout.activity_main)
        
        // Initialize components
        initializeViews()
        initializeViewModel()
        initializeRecyclerView()
        initializeNetworkMonitoring()
        observeViewModel()

        // Keep splash screen visible until audio finishes
        keepSplashScreenVisible()
    }
    private fun keepSplashScreenVisible() {
        splashScreen?.setOnExitAnimationListener { splashScreenView ->
            // Custom exit animation or just remove
            splashScreenView.remove()
        }
    }
    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun initializeViews() {
        videoRecyclerView = findViewById(R.id.video_recycler_view)
        loadingIndicator = findViewById(R.id.loading_indicator)
        errorMessage = findViewById(R.id.error_message)
        
        // Initialize no internet UI elements
        noInternetLayout = findViewById(R.id.no_internet_layout)
        retryButton = noInternetLayout.findViewById(R.id.retry_button)
        wifiIcon = noInternetLayout.findViewById(R.id.wifi_icon)

        // Set up retry button click listener
        retryButton.setOnClickListener {
            checkNetworkAndRetry()
        }
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProvider(this)[VideoViewModel::class.java]
    }

    private fun initializeRecyclerView() {
        // Initialize ExoPlayer pool
        ExoPlayerPool.initialize(this)
        
        // Set up RecyclerView with custom layout manager for 4-direction scrolling
        layoutManager = VideoLayoutManager(this, VideoLayoutManager.VERTICAL, false)
        videoRecyclerView.layoutManager = layoutManager
        
        // Keep default scrolling enabled for vertical gestures
        videoRecyclerView.isNestedScrollingEnabled = true
        
        // Enable snapping for smooth video transitions
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(videoRecyclerView)
        
        // Set up adapter with empty list initially
        videoAdapter = VideoAdapter(mutableListOf(), { video, position ->
            onVideoChanged(video, position)
        }, {
            // Load more videos for infinite scrolling
            viewModel.loadMoreVideos()
        }, { video, position ->
            // Handle video error - auto-advance to next video
            onVideoError(video, position)
        })
        videoRecyclerView.adapter = videoAdapter
        
        // Set up gesture handler for 4-direction scrolling
        gestureHandler = VideoGestureHandler(videoRecyclerView, layoutManager) { direction ->
            handleScrollDirection(direction)
        }
        videoRecyclerView.setOnTouchListener(gestureHandler)
        
        // Set up scroll listener for video changes
        videoRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val currentPosition = getCurrentVideoPosition()
                    if (currentPosition != -1) {
                        videoAdapter.setCurrentPosition(currentPosition)
                        viewModel.setCurrentVideoIndex(currentPosition)
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.videos.observe(this) { videos ->
            if (videos.isNotEmpty()) {
                updateVideoList(videos)
                hideLoading()
                hideError()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
        
        viewModel.isLoadingMore.observe(this) { isLoadingMore ->
            // Handle loading more videos indicator if needed
            if (isLoadingMore) {
                android.util.Log.d("MainActivity", "Loading more videos...")
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                showError(error)
                hideLoading()
            } else {
                hideError()
            }
        }

        viewModel.currentVideoIndex.observe(this) { index ->
            if (index != -1 && index < (viewModel.videos.value?.size ?: 0)) {
                videoRecyclerView.smoothScrollToPosition(index)
            }
        }
    }

    private fun initializeNetworkMonitoring() {
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    if (!isNetworkAvailable) {
                        isNetworkAvailable = true
                        hideNoInternetScreen()
                        android.util.Log.d("MainActivity", "Network available - hiding no internet screen")
                    }
                }
            }
            
            override fun onLost(network: Network) {
                runOnUiThread {
                    if (isNetworkAvailable) {
                        isNetworkAvailable = false
                        showNoInternetScreen()
                        android.util.Log.d("MainActivity", "Network lost - showing no internet screen")
                    }
                }
            }
        }
        
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Check initial network state
        checkInitialNetworkState()
    }
    
    private fun checkInitialNetworkState() {
        isNetworkAvailable = NetworkUtils.isNetworkAvailable(this)
        if (!isNetworkAvailable) {
            showNoInternetScreen()
        }
    }
    
    private fun checkNetworkAndRetry() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            isNetworkAvailable = true
            hideNoInternetScreen()
            // Retry loading videos
            viewModel.refreshVideos()
        } else {
            // Show a brief message that network is still unavailable
//            Toast.makeText(this, "Still no internet connection", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showNoInternetScreen() {
        noInternetLayout.visibility = View.VISIBLE
        videoRecyclerView.visibility = View.GONE
        loadingIndicator.visibility = View.GONE
        errorMessage.visibility = View.GONE
        
        // Start animations
        startNoInternetAnimations()
    }
    
    private fun hideNoInternetScreen() {
        noInternetLayout.visibility = View.GONE
        videoRecyclerView.visibility = View.VISIBLE
        
        // Stop animations
        stopNoInternetAnimations()
    }
    
    private fun startNoInternetAnimations() {
        // WiFi icon pulse animation
        val wifiPulseAnimation = AnimationUtils.loadAnimation(this, R.anim.wifi_pulse)
        wifiIcon.startAnimation(wifiPulseAnimation)
        

        // Button bounce animation
        val buttonBounceAnimation = AnimationUtils.loadAnimation(this, R.anim.button_bounce)
        retryButton.startAnimation(buttonBounceAnimation)
    }
    
    private fun stopNoInternetAnimations() {
        wifiIcon.clearAnimation()

        retryButton.clearAnimation()
    }

    private fun checkNetworkAndShowMessage() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showError("No internet connection. Please check your network and try again.")
        }
    }

    private fun updateVideoList(videos: List<Video>) {
        videoAdapter.updateVideos(videos)
        if (videos.isNotEmpty()) {
            // Ensure the first video is played when the list is updated
            videoAdapter.setCurrentPosition(0)
            viewModel.setCurrentVideoIndex(0)
        }
    }

    private fun onVideoChanged(video: Video, position: Int) {
        // Use intelligent preloading system
        if (isNetworkAvailable) {
            val allVideos = viewModel.videos.value ?: emptyList()
            if (allVideos.isNotEmpty()) {
                val videoUrls = allVideos.map { it.url }
                ExoPlayerPool.preloadVideos(videoUrls, position)
            }
        } else {
            android.util.Log.d("MainActivity", "Network not available, skipping video preloading")
        }
    }
    
    private fun onVideoError(video: Video, position: Int) {
        android.util.Log.w("MainActivity", "Video error for: ${video.title}, removing from list and auto-advancing")
        
        // Remove the failed video from the list
        viewModel.removeVideoAt(position)
        
        // Auto-advance to next video
        val currentIndex = viewModel.currentVideoIndex.value ?: 0
        val totalVideos = viewModel.getVideoCount()
        
        if (totalVideos > 0) {
            // If we removed a video before the current position, adjust the current index
            val adjustedIndex = if (position < currentIndex) {
                currentIndex - 1
            } else if (position == currentIndex) {
                // If we removed the current video, stay at the same index (which now points to the next video)
                currentIndex
            } else {
                currentIndex
            }
            
            // Ensure the index is within bounds
            val nextIndex = if (adjustedIndex >= totalVideos) {
                if (totalVideos > 0) 0 else -1
            } else {
                adjustedIndex
            }
            
            if (nextIndex >= 0) {
                android.util.Log.d("MainActivity", "Auto-advancing to index $nextIndex after removing video at $position")
                
                // Update ViewModel and scroll to next video
                viewModel.setCurrentVideoIndex(nextIndex)
                videoRecyclerView.smoothScrollToPosition(nextIndex)
                videoAdapter.setCurrentPosition(nextIndex)
            }
        } else {
            android.util.Log.w("MainActivity", "No videos left after removing failed video")
        }
    }

    private fun handleScrollDirection(direction: VideoGestureHandler.ScrollDirection) {
        val currentIndex = viewModel.currentVideoIndex.value ?: 0
        val totalVideos = viewModel.getVideoCount()
        
        android.util.Log.d("MainActivity", "Handling scroll direction: $direction, currentIndex: $currentIndex, totalVideos: $totalVideos")
        
        when (direction) {
            VideoGestureHandler.ScrollDirection.UP -> {
                // Move to previous video
                val newIndex = if (currentIndex > 0) currentIndex - 1 else totalVideos - 1
                android.util.Log.d("MainActivity", "Moving UP to index: $newIndex")
//                Toast.makeText(this, "UP - Video ${newIndex + 1}", Toast.LENGTH_SHORT).show()
                viewModel.setCurrentVideoIndex(newIndex)
                videoRecyclerView.smoothScrollToPosition(newIndex)
            }
            VideoGestureHandler.ScrollDirection.DOWN -> {
                // Move to next video
                val newIndex = if (currentIndex < totalVideos - 1) currentIndex + 1 else 0
                android.util.Log.d("MainActivity", "Moving DOWN to index: $newIndex")
//                Toast.makeText(this, "DOWN - Video ${newIndex + 1}", Toast.LENGTH_SHORT).show()
                viewModel.setCurrentVideoIndex(newIndex)
                videoRecyclerView.smoothScrollToPosition(newIndex)
            }
            VideoGestureHandler.ScrollDirection.LEFT -> {
                // Move to previous video (alternative gesture)
                val newIndex = if (currentIndex > 0) currentIndex - 1 else totalVideos - 1
                android.util.Log.d("MainActivity", "Moving LEFT to index: $newIndex")
//                Toast.makeText(this, "LEFT - Video ${newIndex + 1}", Toast.LENGTH_SHORT).show()
                viewModel.setCurrentVideoIndex(newIndex)
                videoRecyclerView.smoothScrollToPosition(newIndex)
            }
            VideoGestureHandler.ScrollDirection.RIGHT -> {
                // Move to next video (alternative gesture)
                val newIndex = if (currentIndex < totalVideos - 1) currentIndex + 1 else 0
                android.util.Log.d("MainActivity", "Moving RIGHT to index: $newIndex")
//                Toast.makeText(this, "RIGHT - Video ${newIndex + 1}", Toast.LENGTH_SHORT).show()
                viewModel.setCurrentVideoIndex(newIndex)
                videoRecyclerView.smoothScrollToPosition(newIndex)
            }
        }
    }

    private fun getCurrentVideoPosition(): Int {
        return layoutManager.findFirstCompletelyVisibleItemPosition()
    }

    private fun showLoading() {
        loadingIndicator.visibility = View.VISIBLE
        videoRecyclerView.visibility = View.GONE
        errorMessage.visibility = View.GONE
    }

    private fun hideLoading() {
        loadingIndicator.visibility = View.GONE
        videoRecyclerView.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
        videoRecyclerView.visibility = View.GONE
    }

    private fun hideError() {
        errorMessage.visibility = View.GONE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Prevent layout recreation on configuration changes
        // This ensures the app stays in landscape mode
    }

    override fun onResume() {
        super.onResume()
        // Check network connectivity
        checkNetworkAndShowMessage()
        // Resume video playback
        ExoPlayerPool.resumeVideo()
    }

    override fun onPause() {
        super.onPause()
        // Pause video playback
        ExoPlayerPool.pauseVideo()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up splash audio
        splashMediaPlayer?.release()
        splashMediaPlayer = null
        // Unregister network callback
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "Error unregistering network callback: ${e.message}")
        }
        
        // Stop animations
        stopNoInternetAnimations()
        
        // Clean up all ViewHolders
        videoAdapter.cleanupRecyclerViewPlayers(videoRecyclerView)
        // Release ExoPlayerPool
        ExoPlayerPool.release()
    }override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullScreen()
        }
    }

}
