package com.github.db1996.taskerha.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

object PingManager {
    private const val PREFS = "app_device"
    private const val KEY_UUID = "device_uuid"
    private const val PING_URL = "https://taskerha-api.db1996-gh.com/ping"
    private const val APP_TOKEN = "rcDyCGAJw0t5Kfl"

    private val client = OkHttpClient()
    private val jsonMedia = "application/json".toMediaType()

    fun clearDeviceId(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_UUID).apply()
    }

    @Synchronized
    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_UUID, null)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_UUID, newId).apply()
        return newId
    }

    suspend fun ping(context: Context) = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext
            val uuid = getOrCreateDeviceId(appContext)
            @Suppress("DEPRECATION")
            val version = appContext.packageManager
                .getPackageInfo(appContext.packageName, 0).versionName ?: "unknown"
            val body = """{"uuid":"$uuid","version":"$version"}""".toRequestBody(jsonMedia)
            val request = Request.Builder()
                .url(PING_URL)
                .post(body)
                .header("x-app-token", APP_TOKEN)
                .build()
            client.newCall(request).execute().use { }
        } catch (_: Exception) {
            // fire-and-forget — ignore all errors
        }
    }
}
