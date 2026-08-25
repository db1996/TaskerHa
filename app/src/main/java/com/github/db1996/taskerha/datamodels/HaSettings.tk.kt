package com.github.db1996.taskerha.datamodels

import android.content.Context
import androidx.core.content.edit
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.logging.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

object HaSettings {
    private const val PREFS = "ha_settings"
    private const val KEY_URL = "ha_url"
    private const val KEY_TOKEN = "ha_token"
    private const val KEY_WS_ENABLED = "ws_enabled"
    private const val KEY_LOG_LEVEL_GENERAL = "log_level_general"
    private const val KEY_LOG_LEVEL_WS = "log_level_ws"

    // Local/home URL feature
    private const val KEY_LOCAL_URL_ENABLED = "local_url_enabled"
    private const val KEY_LOCAL_URL = "ha_local_url"
    private const val KEY_HOME_SSIDS = "home_ssids"

    // Client certificate (mTLS) feature
    private const val KEY_CLIENT_CERT_ENABLED = "client_cert_enabled"
    private const val KEY_CLIENT_CERT_ALIAS = "client_cert_alias"

    // Global request timeout
    private const val KEY_REQUEST_TIMEOUT = "request_timeout"

    fun save(context: Context, url: String, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_URL, url)
                    .putString(KEY_TOKEN, token)
            }
    }

    fun loadUrl(context: Context): String {
        // Compatibility layer: try repository first, fall back to legacy settings
        val activeInstance = HaInstanceRepository.getActive()
        if (activeInstance != null) {
            return activeInstance.remoteUrl
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_URL, "") ?: ""
    }

    // --- Local URL settings ---

    fun saveLocalUrlEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_LOCAL_URL_ENABLED, enabled) }
    }

    fun loadLocalUrlEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_LOCAL_URL_ENABLED, false)
    }

    fun saveLocalUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_LOCAL_URL, url) }
    }

    fun loadLocalUrl(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LOCAL_URL, "") ?: ""
    }

    fun saveHomeSsids(context: Context, ssids: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_HOME_SSIDS, ssids) }
    }

    fun loadHomeSsids(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HOME_SSIDS, emptySet()) ?: emptySet()
    }

    // --- Client certificate settings ---

    fun saveClientCertEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_CLIENT_CERT_ENABLED, enabled) }
    }

    fun loadClientCertEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CLIENT_CERT_ENABLED, false)
    }

    fun saveClientCertAlias(context: Context, alias: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putString(KEY_CLIENT_CERT_ALIAS, alias) }
    }

    fun loadClientCertAlias(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CLIENT_CERT_ALIAS, "") ?: ""
    }

    /**
     * Resolves a single URL to start from. The two branches behave differently:
     *
     * - Instance path (an instance exists in [HaInstanceRepository]): delegates to
     *   [HaInstance.resolveUrl], which returns the *first candidate* of the fallback
     *   chain. That is normally the remote URL even on a home WiFi — the local URL
     *   is a fallback tried by HomeAssistantClient.ping(), not a fast path — unless
     *   the LAN latch is armed for the current SSID, in which case the local URL
     *   comes first. Callers that can follow the chain should use
     *   [HaInstance.resolveUrlCandidates] instead of this.
     * - Legacy fallback (no instance configured): unchanged single-shot behaviour —
     *   if the "local URL" feature is enabled and the device is connected to one of
     *   the configured home WiFi SSIDs, returns the local URL; otherwise the remote
     *   one. No fallback chain exists on this path.
     */
    fun resolveUrl(context: Context): String {
        // Compatibility layer: try repository first, fall back to legacy settings
        val activeInstance = HaInstanceRepository.getActive()
        if (activeInstance != null) {
            return activeInstance.resolveUrl()
        }
        
        // Legacy fallback
        if (!loadLocalUrlEnabled(context)) return loadUrl(context)

        val localUrl = loadLocalUrl(context)
        if (localUrl.isBlank()) return loadUrl(context)

        val homeSsids = loadHomeSsids(context)
        if (homeSsids.isEmpty()) return loadUrl(context)

        val currentSsid = com.github.db1996.taskerha.util.NetworkHelper.getCurrentSsid()
        if (currentSsid != null && homeSsids.contains(currentSsid)) {
            return localUrl
        }

        return loadUrl(context)
    }

    fun loadToken(context: Context): String {
        // Compatibility layer: try repository first, fall back to legacy settings
        val activeInstance = HaInstanceRepository.getActive()
        if (activeInstance != null) {
            return activeInstance.token
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, "") ?: ""
    }
    fun saveWebSocketEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_WS_ENABLED, enabled)
            }
    }

    fun loadWebSocketEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_WS_ENABLED, false)
    }

    fun saveLogLevel(context: Context, channel: LogChannel, level: LogLevel) {
        val key = when (channel) {
            LogChannel.GENERAL -> KEY_LOG_LEVEL_GENERAL
            LogChannel.WEBSOCKET -> KEY_LOG_LEVEL_WS
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit {
                putString(key, level.name)
            }
    }

    fun loadLogLevel(context: Context, channel: LogChannel): LogLevel {
        val (key, def) = when (channel) {
            LogChannel.GENERAL -> KEY_LOG_LEVEL_GENERAL to LogLevel.INFO
            LogChannel.WEBSOCKET -> KEY_LOG_LEVEL_WS to LogLevel.INFO
        }

        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)

        return LogLevel.fromString(raw, def)
    }

    fun saveRequestTimeout(context: Context, seconds: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit { putInt(KEY_REQUEST_TIMEOUT, seconds) }
    }

    fun loadRequestTimeout(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_REQUEST_TIMEOUT, 5)
    }
}

/**
 * Represents a Home Assistant instance configuration.
 * 
 * @param id Short UUID (8 characters) for internal identification
 * @param name Optional user-friendly name
 * @param remoteUrl Remote/internet URL
 * @param localUrl Optional local URL for home network
 * @param token Access token
 * @param homeSsids Set of WiFi SSIDs that trigger local URL usage
 * @param clientCertEnabled Whether to use client certificate authentication
 * @param clientCertAlias Android KeyChain alias for client certificate
 * @param isDefault Whether this is the default instance (fallback)
 */
@Serializable
data class HaInstance(
    val id: String,
    val name: String = "",
    val remoteUrl: String,
    val localUrl: String = "",
    val token: String,
    val homeSsids: Set<String> = emptySet(),
    val clientCertEnabled: Boolean = false,
    val clientCertAlias: String = "",
    val isDefault: Boolean = false,
    val wsEnabled: Boolean = false,
    val hacsAvailable: Boolean = false,
    val hacsChecked: Boolean = false
) {
    /**
     * Ordered list of endpoints to try, first to last. Never empty, and never
     * contains a blank entry unless every configured URL is blank (an unconfigured
     * instance), in which case the single blank remote URL is returned so that
     * [first] stays safe for the call sites that rely on it.
     *
     * The remote endpoint goes first: the local one exists as an offline fallback,
     * not as the fast path. The SSID gate is kept as a precondition — without a
     * matching SSID the local URL is never attempted at all, otherwise on an
     * arbitrary network we would send the access token to whatever host happens to
     * answer at a private address. Because that gate depends on the network the
     * device is on *right now*, this must be called afresh each time the list is
     * needed (HomeAssistantClient invokes it at the top of every ping) and the
     * result must never be cached across pings.
     *
     * The SSID is not an authenticator and this ordering is not a security boundary;
     * see docs/superpowers/specs/2026-08-25-url-fallback-sicurezza-design.md
     *
     * Both parameters default to live state so production callers are unaffected;
     * tests pass them explicitly to keep this function pure.
     */
    fun resolveUrlCandidates(
        currentSsid: String? = com.github.db1996.taskerha.util.NetworkHelper.getCurrentSsid(),
        latchedUrl: String? = com.github.db1996.taskerha.util.LanLatch.armedUrlFor(currentSsid)
    ): List<String> {
        val localEligible = localUrl.isNotBlank() &&
                homeSsids.isNotEmpty() &&
                currentSsid != null &&
                homeSsids.contains(currentSsid)

        val ordered = when {
            !localEligible -> listOf(remoteUrl)
            latchedUrl == localUrl -> listOf(localUrl, remoteUrl)
            else -> listOf(remoteUrl, localUrl)
        }

        // A blank URL is not an endpoint. A local-only instance (blank remoteUrl) is
        // a supported configuration — migrateFromLegacy() produces one whenever only
        // a token was set — and must keep its local endpoint instead of resolving to
        // "" and taking down the WebSocket service. Falling back to listOf(remoteUrl)
        // keeps the list non-empty for the callers that use first().
        return ordered.filter { it.isNotBlank() }.ifEmpty { listOf(remoteUrl) }
    }

    /**
     * The endpoint to start from. Kept for the call sites that only need one URL;
     * the fallback chain lives in [resolveUrlCandidates]. Note this is not
     * necessarily the local URL on a home WiFi — see [resolveUrlCandidates] for the
     * ordering. Parameters default to live state exactly as they do there, and exist
     * so tests can keep this pure.
     */
    fun resolveUrl(
        currentSsid: String? = com.github.db1996.taskerha.util.NetworkHelper.getCurrentSsid(),
        latchedUrl: String? = com.github.db1996.taskerha.util.LanLatch.armedUrlFor(currentSsid)
    ): String = resolveUrlCandidates(currentSsid, latchedUrl).first()

    companion object {
        /**
         * Generates a short 8-character UUID for instance IDs.
         */
        fun generateShortId(): String {
            return UUID.randomUUID().toString().substring(0, 8)
        }
    }
}

/**
 * Repository for managing Home Assistant instances.
 * Singleton initialized in TaskerHaApplication.
 */
object HaInstanceRepository {
    private const val PREFS_NAME = "ha_instances"
    private const val KEY_INSTANCES = "instances"
    private const val KEY_ACTIVE_INSTANCE_ID = "active_instance_id"

    private lateinit var appContext: Context
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _instances = MutableStateFlow<List<HaInstance>>(emptyList())
    val instances: StateFlow<List<HaInstance>> = _instances.asStateFlow()

    private val _activeInstanceId = MutableStateFlow<String?>(null)
    val activeInstanceId: StateFlow<String?> = _activeInstanceId.asStateFlow()

    /**
     * Initialize the repository. Must be called from Application.onCreate().
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        loadInstances()
        loadActiveInstanceId()
    }

    private fun ensureInit() {
        check(::appContext.isInitialized) { "HaInstanceRepository not initialized. Call init() first." }
    }

    private fun loadInstances() {
        ensureInit()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_INSTANCES, null)
        if (jsonString != null) {
            try {
                val list = json.decodeFromString<List<HaInstance>>(jsonString)
                _instances.value = list
            } catch (e: Exception) {
                _instances.value = emptyList()
            }
        } else {
            _instances.value = emptyList()
        }
    }

    private fun loadActiveInstanceId() {
        ensureInit()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _activeInstanceId.value = prefs.getString(KEY_ACTIVE_INSTANCE_ID, null)
    }

    private fun saveInstances() {
        ensureInit()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = json.encodeToString(_instances.value)
        prefs.edit { putString(KEY_INSTANCES, jsonString) }
    }

    private fun saveActiveInstanceId(id: String?) {
        ensureInit()
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_ACTIVE_INSTANCE_ID, id) }
        _activeInstanceId.value = id
    }

    /**
     * Get all instances sorted: default first, then alphabetically by name (empty names first), then by URL.
     */
    fun getAllSorted(): List<HaInstance> {
        return _instances.value.sortedWith(compareBy(
            { !it.isDefault },  // default first
            { it.name.isNotBlank() },  // empty names first among non-defaults
            { it.name.lowercase() },
            { it.remoteUrl.lowercase() }
        ))
    }

    /**
     * Get instance by ID.
     */
    fun getById(id: String): HaInstance? {
        return _instances.value.find { it.id == id }
    }

    /**
     * Get the default instance (fallback for actions without explicit instance).
     */
    fun getDefault(): HaInstance? {
        return _instances.value.find { it.isDefault }
    }

    /**
     * Get the active instance (the one with websocket connection).
     */
    fun getActive(): HaInstance? {
        val id = _activeInstanceId.value ?: return getDefault()
        return getById(id) ?: getDefault()
    }

    /**
     * Add a new instance.
     */
    fun add(instance: HaInstance) {
        ensureInit()
        val list = _instances.value.toMutableList()
        list.add(instance)
        _instances.value = list
        saveInstances()
    }

    /**
     * Update an existing instance.
     */
    fun update(instance: HaInstance) {
        ensureInit()
        val list = _instances.value.toMutableList()
        val index = list.indexOfFirst { it.id == instance.id }
        if (index >= 0) {
            list[index] = instance
            _instances.value = list
            saveInstances()
        }
    }

    /**
     * Delete an instance. Cannot delete the default instance if it's the only one.
     */
    fun delete(id: String): Boolean {
        ensureInit()
        val instance = getById(id) ?: return false
        
        // Cannot delete default if it's the only instance
        if (instance.isDefault && _instances.value.size == 1) {
            return false
        }

        val list = _instances.value.toMutableList()
        list.removeIf { it.id == id }

        // If deleting default, promote another instance
        if (instance.isDefault && list.isNotEmpty()) {
            val newDefault = list.first().copy(isDefault = true)
            val newDefaultIndex = list.indexOfFirst { it.id == newDefault.id }
            list[newDefaultIndex] = newDefault
        }

        // If deleting active instance, clear active ID
        if (_activeInstanceId.value == id) {
            saveActiveInstanceId(null)
        }

        _instances.value = list
        saveInstances()
        return true
    }

    /**
     * Set an instance as default.
     */
    fun setDefault(id: String) {
        ensureInit()
        val list = _instances.value.map {
            it.copy(isDefault = it.id == id)
        }
        _instances.value = list
        saveInstances()
    }

    /**
     * Set the active instance (for websocket connection).
     */
    fun setActive(id: String?) {
        ensureInit()
        saveActiveInstanceId(id)
    }

    /**
     * Check if migration from legacy HaSettings is needed.
     */
    fun needsMigration(): Boolean {
        ensureInit()
        // Check if we have no instances and legacy settings exist
        if (_instances.value.isNotEmpty()) return false
        
        val legacyPrefs = appContext.getSharedPreferences("ha_settings", Context.MODE_PRIVATE)
        val hasLegacyUrl = legacyPrefs.contains("ha_url")
        val hasLegacyToken = legacyPrefs.contains("ha_token")
        
        return hasLegacyUrl || hasLegacyToken
    }

    /**
     * Migrate from legacy HaSettings to new instance-based system.
     */
    fun migrateFromLegacy() {
        ensureInit()
        if (!needsMigration()) return

        val legacyPrefs = appContext.getSharedPreferences("ha_settings", Context.MODE_PRIVATE)
        
        val remoteUrl = legacyPrefs.getString("ha_url", "") ?: ""
        val token = legacyPrefs.getString("ha_token", "") ?: ""
        val localUrl = legacyPrefs.getString("ha_local_url", "") ?: ""
        val homeSsids = legacyPrefs.getStringSet("home_ssids", emptySet()) ?: emptySet()
        val clientCertEnabled = legacyPrefs.getBoolean("client_cert_enabled", false)
        val clientCertAlias = legacyPrefs.getString("client_cert_alias", "") ?: ""

        if (remoteUrl.isNotBlank() || token.isNotBlank()) {
            val instance = HaInstance(
                id = HaInstance.generateShortId(),
                name = "",
                remoteUrl = remoteUrl,
                localUrl = localUrl,
                token = token,
                homeSsids = homeSsids,
                clientCertEnabled = clientCertEnabled,
                clientCertAlias = clientCertAlias,
                isDefault = true
            )
            add(instance)
            setActive(instance.id)

            // Clear legacy settings after successful migration
            legacyPrefs.edit {
                remove("ha_url")
                remove("ha_token")
                remove("ha_local_url")
                remove("home_ssids")
                remove("client_cert_enabled")
                remove("client_cert_alias")
                remove("local_url_enabled")
            }
        }
    }
}
