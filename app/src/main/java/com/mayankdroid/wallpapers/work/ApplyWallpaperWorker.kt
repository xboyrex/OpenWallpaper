package com.mayankdroid.wallpapers.work

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApplyWallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val which = inputData.getInt(KEY_WHICH, WallpaperManager.FLAG_SYSTEM)
        val id = inputData.getString(KEY_ID) ?: "wallpaper"

        val file = File(applicationContext.cacheDir, "apply_$id.jpg")
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty image body")
                body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            }

            // Validate that the downloaded object is actually decodable before touching wallpaper state.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Downloaded file is not a decodable image")

            file.inputStream().use { input ->
                WallpaperManager.getInstance(applicationContext).setStream(input, null, true, which)
            }
            Result.success()
        } catch (t: Throwable) {
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        } finally {
            file.delete()
        }
    }

    companion object {
        const val KEY_URL = "full_url"
        const val KEY_WHICH = "which"
        const val KEY_ID = "id"
    }
}
