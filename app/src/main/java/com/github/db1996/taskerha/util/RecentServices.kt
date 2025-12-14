package com.github.db1996.taskerha.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HasServiceKeys {
    fun serviceKeys(): List<String>
}

object ServiceRecents {
    private const val PREFS = "service_recents"
    private const val KEY = "ids"
    private const val MAX = 30

    private lateinit var prefs: SharedPreferences

    private val _recents = MutableStateFlow<List<String>>(emptyList())
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _recents.value = load()
    }

    fun add(key: String) = addAll(listOf(key))

    fun addAll(keys: List<String>) {
        ensureInit()
        val cleaned = keys.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return

        val current = load().toMutableList()
        for (k in cleaned) {
            current.remove(k)
            current.add(0, k)
        }

        val limited = current.take(MAX)
        prefs.edit().putString(KEY, limited.joinToString("\n")).apply()
        _recents.value = limited
    }

    fun remove(key: String) {
        ensureInit()
        val k = key.trim()
        if (k.isBlank()) return

        val current = load().toMutableList()
        val changed = current.remove(k)
        if (!changed) return

        prefs.edit().putString(KEY, current.joinToString("\n")).apply()
        _recents.value = current
    }

    fun clear() {
        ensureInit()
        prefs.edit().remove(KEY).apply()
        _recents.value = emptyList()
    }

    private fun load(): List<String> =
        prefs.getString(KEY, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun ensureInit() {
        check(::prefs.isInitialized) { "ServiceRecents.init(context) not called" }
    }
}
