package com.mayankdroid.wallpapers.data

import com.mayankdroid.wallpapers.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

class WallpaperRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun fetch(): List<Wallpaper> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(AppConfig.FEED_URL)
            .header("Cache-Control", "no-cache")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Feed HTTP ${response.code}")
            val body = response.body.string()
            parse(body)
        }
    }

    private fun parse(json: String): List<Wallpaper> {
        val root = JSONArray(json)
        return buildList(root.length()) {
            for (i in 0 until root.length()) {
                val o = root.getJSONObject(i)
                val full = o.getString("full_url")
                val thumb = o.getString("thumb_url")
                require(full.startsWith("https://")) { "full_url must use HTTPS" }
                require(thumb.startsWith("https://")) { "thumb_url must use HTTPS" }
                add(
                    Wallpaper(
                        id = o.getString("id"),
                        title = o.optString("title", "Untitled"),
                        category = o.optString("category", "General"),
                        width = o.optInt("width", 3840),
                        height = o.optInt("height", 2160),
                        fullUrl = full,
                        thumbUrl = thumb,
                        sizeBytes = o.optLong("size_bytes", 0),
                        updatedAt = o.optString("updated_at", "")
                    )
                )
            }
        }
    }
}
