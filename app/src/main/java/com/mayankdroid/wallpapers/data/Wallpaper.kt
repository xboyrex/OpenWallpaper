package com.mayankdroid.wallpapers.data

data class Wallpaper(
    val id: String,
    val title: String,
    val category: String,
    val width: Int,
    val height: Int,
    val fullUrl: String,
    val thumbUrl: String,
    val sizeBytes: Long,
    val updatedAt: String
)
