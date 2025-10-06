package com.saadho.funnutv.data

import com.google.gson.annotations.SerializedName

/**
 * Response wrapper for video list from JSON
 */
data class VideoResponse(
    @SerializedName("videos")
    val videos: List<Video>
)
