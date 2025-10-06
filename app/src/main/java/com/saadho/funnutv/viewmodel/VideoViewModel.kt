package com.saadho.funnutv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.saadho.funnutv.data.Video
import com.saadho.funnutv.data.VideoRepository
import com.saadho.funnutv.NetworkUtils
import kotlinx.coroutines.launch

/**
 * ViewModel for managing video data and state
 */
class VideoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = VideoRepository(application)
    
    private val _videos = MutableLiveData<List<Video>>()
    val videos: LiveData<List<Video>> = _videos
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _currentVideoIndex = MutableLiveData<Int>()
    val currentVideoIndex: LiveData<Int> = _currentVideoIndex
    
    private val allVideos = mutableListOf<Video>()
    private var currentLoadIndex = 0
    private val loadBatchSize = 20
    
    init {
        loadVideos()
        _currentVideoIndex.value = 0
    }
    
    /**
     * Load initial videos for infinite scrolling
     */
    private fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Check network connectivity before loading videos
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _error.value = "No internet connection available"
                _isLoading.value = false
                return@launch
            }
            
            repository.getInfiniteVideos(0, loadBatchSize)
                .onSuccess { videoList ->
                    allVideos.clear()
                    allVideos.addAll(videoList)
                    _videos.value = allVideos.toList()
                    currentLoadIndex = loadBatchSize
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load videos"
                }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Load more videos for infinite scrolling
     */
    fun loadMoreVideos() {
        viewModelScope.launch {
            _isLoadingMore.value = true
            
            // Check network connectivity before loading more videos
            if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                _error.value = "No internet connection available"
                _isLoadingMore.value = false
                return@launch
            }
            
            repository.getInfiniteVideos(currentLoadIndex, loadBatchSize)
                .onSuccess { videoList ->
                    allVideos.addAll(videoList)
                    _videos.value = allVideos.toList()
                    currentLoadIndex += loadBatchSize
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Failed to load more videos"
                }
            
            _isLoadingMore.value = false
        }
    }
    
    /**
     * Get the current video
     */
    fun getCurrentVideo(): Video? {
        val videos = _videos.value ?: return null
        val index = _currentVideoIndex.value ?: 0
        return if (index in videos.indices) videos[index] else null
    }
    
    /**
     * Get the next video
     */
    fun getNextVideo(): Video? {
        val videos = _videos.value ?: return null
        val index = _currentVideoIndex.value ?: 0
        val nextIndex = (index + 1) % videos.size
        return videos[nextIndex]
    }
    
    /**
     * Get the previous video
     */
    fun getPreviousVideo(): Video? {
        val videos = _videos.value ?: return null
        val index = _currentVideoIndex.value ?: 0
        val prevIndex = if (index == 0) videos.size - 1 else index - 1
        return videos[prevIndex]
    }
    
    /**
     * Get the next N videos for preloading
     */
    fun getNextVideos(count: Int): List<Video> {
        val currentIndex = _currentVideoIndex.value ?: return emptyList()
        val videos = _videos.value ?: return emptyList()
        if (videos.isEmpty()) return emptyList()
        
        val nextVideos = mutableListOf<Video>()
        for (i in 1..count) {
            val nextIndex = (currentIndex + i) % videos.size
            videos.getOrNull(nextIndex)?.let { nextVideos.add(it) }
        }
        return nextVideos
    }
    
    /**
     * Move to the next video
     */
    fun moveToNextVideo() {
        val videos = _videos.value ?: return
        val currentIndex = _currentVideoIndex.value ?: 0
        val nextIndex = (currentIndex + 1) % videos.size
        _currentVideoIndex.value = nextIndex
    }
    
    /**
     * Move to the previous video
     */
    fun moveToPreviousVideo() {
        val videos = _videos.value ?: return
        val currentIndex = _currentVideoIndex.value ?: 0
        val prevIndex = if (currentIndex == 0) videos.size - 1 else currentIndex - 1
        _currentVideoIndex.value = prevIndex
    }
    
    /**
     * Set the current video index
     */
    fun setCurrentVideoIndex(index: Int) {
        val videos = _videos.value ?: return
        if (index in videos.indices) {
            _currentVideoIndex.value = index
        }
    }
    
    /**
     * Get video at specific index
     */
    fun getVideoAt(index: Int): Video? {
        val videos = _videos.value ?: return null
        return if (index in videos.indices) videos[index] else null
    }
    
    /**
     * Get total number of videos
     */
    fun getVideoCount(): Int {
        return _videos.value?.size ?: 0
    }
    
    /**
     * Refresh videos
     */
    fun refreshVideos() {
        loadVideos()
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}
