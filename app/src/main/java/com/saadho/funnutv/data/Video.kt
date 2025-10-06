package com.saadho.funnutv.data

import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

/**
 * Data class representing a video item
 * Compatible with both Gson and Firestore serialization
 */
data class Video(
    @SerializedName("id")
    @PropertyName("id")
    val id: String = "",
    @SerializedName("title")
    @PropertyName("title")
    val title: String = "",
    @SerializedName("url")
    @PropertyName("url")
    val url: String = "",
    @SerializedName("thumbnail")
    @PropertyName("thumbnail")
    val thumbnail: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "")
}
