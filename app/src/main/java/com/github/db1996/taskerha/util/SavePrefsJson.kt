package com.github.db1996.taskerha.util

import android.content.Context
import android.content.SharedPreferences
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.tasker.base.BaseLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

interface SavePrefsJson {
    /** Which SharedPreferences file to store in */
    fun prefsName(): String

    /** The JSON string to store (one line recommended) */
    fun jsonValue(): String
}


object PrefsJsonStore : BaseLogger {
    private const val KEY_ITEMS = "items"

    override val logTag: String get() = "PrefsJsonStore"

    private lateinit var appContext: Context

    private val flows = ConcurrentHashMap<String, MutableStateFlow<Set<String>>>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun observe(prefsName: String): StateFlow<Set<String>> {
        ensureInit()
        return flows.getOrPut(prefsName) { MutableStateFlow(load(prefsName)) }.asStateFlow()
    }

    fun add(item: SavePrefsJson) = add(item.prefsName(), item.jsonValue())

    fun add(prefsName: String, json: String) {
        ensureInit()
        val cleaned = json.trim().replace("\n", "")
        if (cleaned.isBlank()) return

        val prefs = prefs(prefsName)
        val current = (prefs.getStringSet(KEY_ITEMS, emptySet()) ?: emptySet()).toMutableSet()

        val changed = current.add(cleaned) // dedupe
        if (!changed) return

        prefs.edit().putStringSet(KEY_ITEMS, current).apply()
        CustomLogger.e("PrefsJsonStore", "added $json to $prefsName")
        flows.getOrPut(prefsName) { MutableStateFlow(emptySet()) }.value = current
    }

    fun remove(prefsName: String, json: String) {
        ensureInit()
        val cleaned = json.trim().replace("\n", "")
        if (cleaned.isBlank()) return

        val prefs = prefs(prefsName)
        val current = (prefs.getStringSet(KEY_ITEMS, emptySet()) ?: emptySet()).toMutableSet()
        val changed = current.remove(cleaned)
        if (!changed) return

        prefs.edit().putStringSet(KEY_ITEMS, current).apply()
        flows.getOrPut(prefsName) { MutableStateFlow(emptySet()) }.value = current
    }

    fun clear(prefsName: String) {
        ensureInit()
        prefs(prefsName).edit().remove(KEY_ITEMS).apply()
        flows.getOrPut(prefsName) { MutableStateFlow(emptySet()) }.value = emptySet()
    }

    fun reload(prefsName: String) {
        ensureInit()
        flows.getOrPut(prefsName) { MutableStateFlow(emptySet()) }.value = load(prefsName)
    }

    private fun load(prefsName: String): Set<String> =
        (prefs(prefsName).getStringSet(KEY_ITEMS, emptySet()) ?: emptySet())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

    private fun prefs(name: String): SharedPreferences =
        appContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun ensureInit() {
        check(::appContext.isInitialized) { "PrefsJsonStore.init(context) not called" }
    }
}