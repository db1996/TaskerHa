package com.github.db1996.taskerha.logging

import android.content.Context
import android.util.Log
import com.github.db1996.taskerha.datamodels.HaSettings
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider


object CustomLogger {

    private const val GENERAL_BASE = "taskerha.log"
    private const val WS_BASE = "taskerha_ws.log"

    private const val MAX_BYTES = 512 * 1024 // 512KB per file
    private const val MAX_ROTATIONS = 10

    private val lockGeneral = Any()
    private val lockWs = Any()

    private var appContext: Context? = null

    // Cached thresholds
    private val generalMinLevel = AtomicReference(LogLevel.DEBUG)
    private val wsMinLevel = AtomicReference(LogLevel.DEBUG)

    fun init(context: Context) {
        appContext = context.applicationContext
        generalMinLevel.set(HaSettings.loadLogLevel(context, LogChannel.GENERAL))
        wsMinLevel.set(HaSettings.loadLogLevel(context, LogChannel.WEBSOCKET))
    }

    fun setMinLevel(channel: LogChannel, minLevel: LogLevel) {
        when (channel) {
            LogChannel.GENERAL -> generalMinLevel.set(minLevel)
            LogChannel.WEBSOCKET -> wsMinLevel.set(minLevel)
        }
    }

    // Convenience methods
    fun e(tag: String, msg: String, channel: LogChannel = LogChannel.GENERAL, t: Throwable? = null) =
        log(channel, LogLevel.ERROR, tag, msg, t)

    fun w(tag: String, msg: String, channel: LogChannel = LogChannel.GENERAL, t: Throwable? = null) =
        log(channel, LogLevel.WARN, tag, msg, t)

    fun i(tag: String, msg: String, channel: LogChannel = LogChannel.GENERAL) =
        log(channel, LogLevel.INFO, tag, msg, null)

    fun d(tag: String, msg: String, channel: LogChannel = LogChannel.GENERAL) =
        log(channel, LogLevel.DEBUG, tag, msg, null)

    fun v(tag: String, msg: String, channel: LogChannel = LogChannel.GENERAL) =
        log(channel, LogLevel.VERBOSE, tag, msg, null)

    fun log(channel: LogChannel, level: LogLevel, tag: String, msg: String, t: Throwable?) {
        when (level) {
            LogLevel.ERROR -> Log.e(tag, msg, t)
            LogLevel.WARN -> Log.w(tag, msg, t)
            LogLevel.INFO -> Log.i(tag, msg, t)
            LogLevel.DEBUG -> Log.d(tag, msg, t)
            LogLevel.VERBOSE -> Log.v(tag, msg, t)
            LogLevel.OFF -> { /* no-op */ }
        }

        val ctx = appContext ?: return
        if (level == LogLevel.OFF) return

        val min = when (channel) {
            LogChannel.GENERAL -> generalMinLevel.get()
            LogChannel.WEBSOCKET -> wsMinLevel.get()
        }

        if (!min.allows(level)) return

        try {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val line = buildString {
                append(ts).append(" ")
                append(level.name).append(" ")
                append(tag).append(": ")
                append(msg)
                if (t != null) append("\n").append(Log.getStackTraceString(t))
                append("\n")
            }

            val base = when (channel) {
                LogChannel.GENERAL -> GENERAL_BASE
                LogChannel.WEBSOCKET -> WS_BASE
            }
            val lock = when (channel) {
                LogChannel.GENERAL -> lockGeneral
                LogChannel.WEBSOCKET -> lockWs
            }

            val dir = ctx.filesDir ?: return

            synchronized(lock) {
                rotateIfNeeded(dir, base)
                val file = File(dir, base) // always write to base after rotation check
                FileWriter(file, true).use { it.append(line) }
            }
        } catch (_: Throwable) {
            // never crash due to logging
            Log.e("CustomLogger", "log failed", t)
        }
    }

    fun getLogFiles(channel: LogChannel? = null): List<File> {
        val dir = appContext?.filesDir ?: return emptyList()
        return when (channel) {
            LogChannel.GENERAL -> listFilesForBase(dir, GENERAL_BASE)
            LogChannel.WEBSOCKET -> listFilesForBase(dir, WS_BASE)
            null -> listFilesForBase(dir, GENERAL_BASE) + listFilesForBase(dir, WS_BASE)
        }
    }

    fun clear(channel: LogChannel? = null) {
        val dir = appContext?.filesDir ?: return

        fun clearBase(base: String, lock: Any) {
            synchronized(lock) {
                val highest = findHighestRotationIndex(dir, base)
                for (i in 0..highest) {
                    runCatching { rotatedFile(dir, base, i).delete() }
                }

                Toast.makeText(appContext, "Logs cleared $base", Toast.LENGTH_SHORT).show()
            }
        }

        when (channel) {
            LogChannel.GENERAL -> clearBase(GENERAL_BASE, lockGeneral)
            LogChannel.WEBSOCKET -> clearBase(WS_BASE, lockWs)
            null -> {
                clearBase(GENERAL_BASE, lockGeneral)
                clearBase(WS_BASE, lockWs)
            }
        }
    }

    // ===== Rotation helpers (auto-numbered) =====

    private fun rotatedFile(dir: File, base: String, index: Int): File =
        if (index == 0) File(dir, base) else File(dir, "$base.$index")

    private fun findHighestRotationIndex(dir: File, base: String): Int {
        var max = 0
        dir.listFiles()?.forEach { f ->
            val name = f.name
            if (name == base) return@forEach
            if (name.startsWith("$base.")) {
                val suffix = name.substringAfter("$base.", "")
                val n = suffix.toIntOrNull()
                if (n != null && n > max) max = n
            }
        }
        return max
    }

    private fun rotateIfNeeded(dir: File, base: String) {
        val baseFile = rotatedFile(dir, base, 0)
        if (!baseFile.exists()) return
        if (baseFile.length() <= MAX_BYTES) return

        val highest = findHighestRotationIndex(dir, base)
        val next = highest + 1

        runCatching { baseFile.renameTo(rotatedFile(dir, base, next)) }

        val cutoff = next - MAX_ROTATIONS
        if (cutoff > 0) {
            for (i in 1..cutoff) {
                runCatching { rotatedFile(dir, base, i).delete() }
            }
        }
    }

    private fun listFilesForBase(dir: File, base: String): List<File> {
        val highest = findHighestRotationIndex(dir, base)
        return (0..highest)
            .map { rotatedFile(dir, base, it) }
            .filter { it.exists() }
    }

    fun buildShareLogsIntent(context: Context, channel: LogChannel? = null): Intent? {
        val files = getLogFiles(channel)
        if (files.isEmpty()) return null

        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>(files.size)

        for (f in files) {
            uris.add(FileProvider.getUriForFile(context, authority, f))
        }

        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/plain"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}