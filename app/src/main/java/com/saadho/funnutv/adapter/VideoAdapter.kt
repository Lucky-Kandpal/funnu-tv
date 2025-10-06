package com.saadho.funnutv.adapter

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.saadho.funnutv.R
import com.saadho.funnutv.data.Video
import com.saadho.funnutv.player.ExoPlayerPool
import com.saadho.funnutv.NetworkUtils
import androidx.media3.common.PlaybackException
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Adapter for the video feed RecyclerView
 * Handles video playback and view recycling
 */

class VideoAdapter(
    private val videos: MutableList<Video>,
    private val onVideoChanged: (Video, Int) -> Unit,
    private val onLoadMore: () -> Unit,
    private val onVideoError: (Video, Int) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var currentPosition = 0
    private var playerView: PlayerView? = null
    private val preloadThreshold = 3 // Preload next 3 videos

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.bind(video, position == currentPosition)
        
        // Check if we need to load more videos for infinite scrolling
        if (position >= videos.size - preloadThreshold) {
            onLoadMore()
        }
    }

    override fun getItemCount(): Int = videos.size

    /**
     * Set the current playing position
     */
    fun setCurrentPosition(position: Int) {
        val oldPosition = currentPosition
        currentPosition = position
        
        // Notify the old and new positions to update their UI
        notifyItemChanged(oldPosition)
        notifyItemChanged(currentPosition)
        
        // Trigger video change callback
        if (position in videos.indices) {
            onVideoChanged(videos[position], position)
        }
    }

    /**
     * Update the video list
     */
    fun updateVideos(newVideos: List<Video>) {
        videos.clear()
        videos.addAll(newVideos)
        notifyDataSetChanged()
    }
    
    /**
     * Add more videos to the list for infinite scrolling
     */
    fun addMoreVideos(newVideos: List<Video>) {
        val oldSize = videos.size
        videos.addAll(newVideos)
        notifyItemRangeInserted(oldSize, newVideos.size)
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playPauseOverlay: ImageView = itemView.findViewById(R.id.play_pause_overlay)
        private val thumbnailView: ImageView = itemView.findViewById(R.id.thumbnail_view)
        private val hidePlayButtonHandler = Handler(Looper.getMainLooper())
        private val hidePlayButtonRunnable = Runnable {
            playPauseOverlay.visibility = View.GONE
        }

        private val playerContainer: FrameLayout = itemView.findViewById(R.id.player_container)
        private val videoLoading: ProgressBar = itemView.findViewById(R.id.video_loading)
        private val videoTitle: TextView = itemView.findViewById(R.id.video_title)
        
        init {
            // Apply curved corners to thumbnail view
            thumbnailView.clipToOutline = true
//            thumbnailView.outlineProvider = object : android.view.ViewOutlineProvider() {
//                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
//                    val cornerRadius = 24f * itemView.context.resources.displayMetrics.density
//                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
//                }
//            }
            
            // Apply curved corners to player container
            playerContainer.clipToOutline = true
            playerContainer.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    val cornerRadius = 24f * itemView.context.resources.displayMetrics.density
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                }
            }
        }

        fun bind(video: Video, isCurrentVideo: Boolean) {
            // Set video title
            videoTitle.text = video.title
            videoTitle.visibility = View.GONE

            // Load thumbnail image
            loadThumbnail(video.thumbnail)

            if (isCurrentVideo) {
                // This is the current video - set up player
                setupPlayer(video)
                playPauseOverlay.visibility = View.GONE
                // Keep thumbnail visible until video is ready
                thumbnailView.visibility = View.VISIBLE
            } else {
                // This is not the current video - clear player and show thumbnail
                clearPlayer()
                playPauseOverlay.visibility = View.GONE // Don't show play icon by default
                thumbnailView.visibility = View.VISIBLE
            }

            // Add click listener for play/pause functionality
            itemView.setOnClickListener {
                if (isCurrentVideo) {
                    togglePlayPause()
                } else {
                    // Switch to this video
                    onVideoChanged(video, adapterPosition)
                }
            }
        }




        private fun togglePlayPause() {
            val player = ExoPlayerPool.getPlayer()
            if (player != null) {
                if (player.isPlaying) {
                    ExoPlayerPool.pauseVideo()
                } else {
                    ExoPlayerPool.resumeVideo()
                }
                // Show overlay every time user taps
                showTemporaryPlayButton()
            }
        }

        private fun showTemporaryPlayButton() {
            playPauseOverlay.visibility = View.VISIBLE
            playPauseOverlay.setImageResource(R.drawable.baseline_play_arrow_24)

            hidePlayButtonHandler.removeCallbacks(hidePlayButtonRunnable)
            hidePlayButtonHandler.postDelayed(hidePlayButtonRunnable, 1300)
        }
        private fun showPlayButton() {
            playPauseOverlay.visibility = View.VISIBLE
            playPauseOverlay.setImageResource(R.drawable.baseline_play_arrow_24)
        }



        private fun loadThumbnail(thumbnailUrl: String) {
            try {
                // Check network connectivity before loading thumbnail
                if (!NetworkUtils.isNetworkAvailable(itemView.context)) {
                    android.util.Log.w("VideoAdapter", "No network available, using fallback thumbnail")
                    thumbnailView.setImageResource(R.drawable.kids_feed_bg)
                    return
                }
                
                Glide.with(itemView.context)
                    .load(thumbnailUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.kids_feed_bg)
                    .error(R.drawable.kids_feed_bg)
                    .fallback(R.drawable.kids_feed_bg)
                    .timeout(10000) // 10 second timeout
                    .into(thumbnailView)
            } catch (e: Exception) {
                android.util.Log.w("VideoAdapter", "Error loading thumbnail: $thumbnailUrl, error: ${e.message}")
                // Set fallback thumbnail
                thumbnailView.setImageResource(R.drawable.kids_feed_bg)
            }
        }

        @OptIn(UnstableApi::class)
        private fun setupPlayer(video: Video) {
            // Remove existing player view
            playerContainer.removeAllViews()

            // Create a FrameLayout to wrap the PlayerView
            val frameLayout = FrameLayout(itemView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                clipChildren = true
                clipToPadding = true

                // Create a GradientDrawable for the border with rounded corners
                val borderColor = Color.parseColor("#F4C2C2") // Baby pink color
                val borderWidth = 8 // Border width in pixels
                val cornerRadius = 24f * itemView.context.resources.displayMetrics.density // Corner radius in pixels

                val borderDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.TRANSPARENT) // Transparent background to show the border
                    setStroke(borderWidth, borderColor)
                    setCornerRadius(cornerRadius)
                }

                background = borderDrawable
            }

            // Create the PlayerView
            playerView = PlayerView(itemView.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)

                // Force TextureView and rounded corners
                setPlayerSurfaceTypeToTextureView()
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        val cornerRadius = 24f * itemView.context.resources.displayMetrics.density
                        outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
                    }
                }

                // Ensure video fills the entire PlayerView area
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                // Set a baby pink border
                val borderColor = Color.parseColor("#F4C2C2") // Baby pink color
                val borderWidth = 8 // Border width in pixels
                val borderDrawable = GradientDrawable().apply {
                    setStroke(borderWidth, borderColor)
                    setColor(Color.TRANSPARENT) // Set background to transparent
                    cornerRadius = 24f * itemView.context.resources.displayMetrics.density
                }
                background = borderDrawable
            }

            // Add the PlayerView to the FrameLayout
            frameLayout.addView(playerView)
            playerContainer.addView(frameLayout)

            // Set player to the player view
            ExoPlayerPool.getPlayer()?.let { player ->
                playerView?.player = player

                // Add listener to handle playback states and errors
                val listener = object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                videoLoading.visibility = View.GONE
                                // Hide thumbnail only when video is ready and playing
                                if (player.isPlaying) {
                                    thumbnailView.visibility = View.GONE
                                }
                            }
                            Player.STATE_BUFFERING -> {
                                videoLoading.visibility = View.VISIBLE
                                // Keep thumbnail visible while buffering
                                thumbnailView.visibility = View.VISIBLE
                            }
                            Player.STATE_ENDED -> {
                                videoLoading.visibility = View.GONE
                                showPlayButton()
                                // Show thumbnail when video ends
                                thumbnailView.visibility = View.VISIBLE
                            }
                            Player.STATE_IDLE -> {
                                videoLoading.visibility = View.GONE
                                // Show thumbnail when video is idle
                                thumbnailView.visibility = View.VISIBLE
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            thumbnailView.visibility = View.GONE
                            playPauseOverlay.visibility = View.GONE // hide overlay if visible
                        } else {
                            thumbnailView.visibility = View.VISIBLE
                            hidePlayButtonHandler.removeCallbacks(hidePlayButtonRunnable)
                            playPauseOverlay.visibility = View.GONE // never show overlay automatically
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        videoLoading.visibility = View.GONE
                        // Handle playback error
                    }
                }

                player.addListener(listener)

                // Play the video
                ExoPlayerPool.playVideo(video.url)
            }
        }
        private fun clearPlayer() {
            playerContainer.removeAllViews()
            playerView = null
            videoLoading.visibility = View.GONE
            // Ensure thumbnail is visible when player is cleared
            thumbnailView.visibility = View.VISIBLE
            // Don't show play button by default
            playPauseOverlay.visibility = View.GONE
        }

        fun cleanup() {
            hidePlayButtonHandler.removeCallbacks(hidePlayButtonRunnable)
            playerView?.player = null
            playerView = null
        }
    }

    /**
     * Clean up resources when adapter is destroyed
     */
    @OptIn(UnstableApi::class)
    private fun PlayerView.setPlayerSurfaceTypeToTextureView() {
        // Force TextureView instead of SurfaceView for rounded corners
        this.videoSurfaceView?.let { surfaceView ->
            if (surfaceView is SurfaceView) {
                val textureView = TextureView(context)
                textureView.layoutParams = surfaceView.layoutParams
                textureView.id = surfaceView.id

                // Remove the old SurfaceView
                val index = this.indexOfChild(surfaceView)
                this.removeView(surfaceView)

                // Add TextureView in the same position
                this.addView(textureView, index)

                // Tell the PlayerView to render video here
                this.setVideoTextureView(textureView)
            }
        }
    }
    @Suppress("DiscouragedPrivateApi")
    private fun PlayerView.setVideoTextureView(textureView: TextureView) {
        try {
            val method = PlayerView::class.java.getDeclaredMethod("setVideoTextureView", TextureView::class.java)
            method.isAccessible = true
            method.invoke(this, textureView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun cleanupRecyclerViewPlayers(recyclerView: RecyclerView) {
        for (i in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i))
            if (holder is VideoViewHolder) {
                holder.cleanup()
            }
        }
    }
}
