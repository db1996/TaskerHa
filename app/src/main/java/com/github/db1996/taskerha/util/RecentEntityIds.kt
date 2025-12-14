package com.github.db1996.taskerha.util

import android.content.Context
import com.github.db1996.taskerha.tasker.base.BaseLogger
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HasEntityIds {
    fun entityIds(): List<String>
}

object EntityRecents: BaseLogger {
    private const val PREFS = "entity_recents"
    private const val KEY = "ids"
    private const val MAX = 30

    override val logTag: String
        get() = "EntityRecentsStore"

    private lateinit var prefs: SharedPreferences

    private val _recents = MutableStateFlow<List<String>>(emptyList())
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _recents.value = load()
    }

    fun add(entityId: String) = addAll(listOf(entityId))

    fun addAll(ids: List<String>) {
        ensureInit()
        val cleaned = ids.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return

        val current = load().toMutableList()

        for (id in cleaned) {
            current.remove(id)
            current.add(0, id)
        }

        val limited = current.take(MAX)
        prefs.edit().putString(KEY, limited.joinToString("\n")).apply()
        _recents.value = limited
    }

    fun remove(entityId: String) {
        ensureInit()
        val id = entityId.trim()
        if (id.isBlank()) return

        val current = load().toMutableList()
        val changed = current.remove(id)
        if (!changed) return

        prefs.edit().putString(KEY, current.joinToString("\n")).apply()
        _recents.value = current
    }

    fun clear() {
        ensureInit()
        prefs.edit().remove(KEY).apply()
        _recents.value = emptyList()
    }

    fun reload() {
        ensureInit()
        _recents.value = load()
    }

    private fun load(): List<String> =
        prefs.getString(KEY, "")?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()

    private fun ensureInit() {
        check(::prefs.isInitialized) { "EntityRecents.init(context) not called" }
    }
}