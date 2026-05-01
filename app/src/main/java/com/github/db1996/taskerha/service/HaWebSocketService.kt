package com.github.db1996.taskerha.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.github.db1996.taskerha.R
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.service.data.HaForDuration
import com.github.db1996.taskerha.service.data.HaMessageWsEnvelope
import com.github.db1996.taskerha.service.data.OnSmallMessageEvent
import com.github.db1996.taskerha.service.data.OnTriggerStateEnvelope
import com.github.db1996.taskerha.service.data.StateTrigger
import com.github.db1996.taskerha.service.data.SubscribeTriggerRequest
import com.github.db1996.taskerha.tasker.base.BaseLogger
import com.github.db1996.taskerha.tasker.onHaMessage.triggerOnHaMessageHelper2
import com.github.db1996.taskerha.tasker.ontriggerstate.data.OnTriggerStateBuiltForm
import com.github.db1996.taskerha.tasker.ontriggerstate.triggerOnTriggerStateEvent2
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.github.db1996.taskerha.util.HaHttpClientFactory
import com.github.db1996.taskerha.util.NetworkHelper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger


class HaWebSocketService : Service(), BaseLogger {
    private val json = Json { ignoreUnknownKeys = true }
    private val TRIGGER_STATE_EVENT_ID = 2

    // Ever-increasing message ID counter for WS messages (HA requires strictly increasing IDs)
    private val messageIdCounter = AtomicInteger(TRIGGER_STATE_EVENT_ID)

    // Maps WS subscription message ID -> triggerId (empty string = legacy batch subscription)
    // Only IDs present in this map are considered active; events with unknown IDs are ignored.
    private val wsSubIdToTriggerId = ConcurrentHashMap<Int, String>()

    override val logTag: String
        get() = "HaWebSocketService"

    override val logChannel: LogChannel
        get() = LogChannel.WEBSOCKET

    companion object {
        const val TAG = "HaWebSocketService"
        const val CHANNEL_ID = "ha_websocket_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_RESUBSCRIBE_TRIGGERS = "com.github.db1996.taskerha.RESUBSCRIBE_TRIGGERS"

        @RequiresApi(Build.VERSION_CODES.O)
        fun start(context: Context) {
            CustomLogger.i(TAG, "Attempting to start websocket", LogChannel.WEBSOCKET)

            val intent = Intent(context, HaWebSocketService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HaWebSocketService::class.java)
            context.stopService(intent)
        }

        fun resubscribeTriggers(context: Context) {
            val intent = Intent(context, HaWebSocketService::class.java).apply {
                action = ACTION_RESUBSCRIBE_TRIGGERS
            }
            context.startService(intent)
        }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        logError("coroutine crashed", t)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    private var webSocket: WebSocket? = null
    private var wifiRegistration: NetworkHelper.ChangeListenerRegistration? = null
    private var lastResolvedUrl: String? = null

    private val httpClient: OkHttpClient by lazy {
        Logger.getLogger(OkHttpClient::class.java.getName()).setLevel(Level.FINE)
        HaHttpClientFactory.build(this) { it.pingInterval(30, TimeUnit.SECONDS) }
    }

    // === Reconnect state ===
    private val baseReconnectDelayMs = 1_000L  // 1 second
    private val maxReconnectDelayMs = 60_000L  // 60 seconds
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    @Volatile
    private var isShuttingDown = false

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESUBSCRIBE_TRIGGERS) {
            doResubscribeTriggers()
            return START_STICKY
        }

        if (!startForegroundSafely("Connecting to Home Assistant...")) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!isShuttingDown && webSocket == null && reconnectJob?.isActive != true) {
            logInfo("Starting websocket service from onStartCommand")
            runCatching { connectWebSocket() }
                .onFailure { t -> logError("connectWebSocket failed from onStartCommand", t) }
        }

        // Monitor WiFi changes to reconnect with the correct URL when switching networks.
        // NetworkHelper has its own long-lived monitoring; we just listen for changes.
        if (wifiRegistration == null && HaSettings.loadLocalUrlEnabled(this)) {
            NetworkHelper.startMonitoring(this)
            wifiRegistration = NetworkHelper.addChangeListener {
                onWifiChanged()
            }
        }

        return START_STICKY
    }

    @RequiresApi(35)
    override fun onTimeout(startId: Int, fgsType: Int) {
        logError("FGS onTimeout called (fgsType=$fgsType) -> stopping")
        stopSelf()
    }


    @RequiresApi(Build.VERSION_CODES.S)
    private fun startForegroundSafely(text: String): Boolean {
        val notification = buildNotification(text)

        return try {
            if (Build.VERSION.SDK_INT >= 34) {
                androidx.core.app.ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: android.app.ForegroundServiceStartNotAllowedException) {
            logError("startForeground blocked: ${e.message}", e)
            false
        } catch (t: Throwable) {
            logError("startForeground failed", t)
            false
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        isShuttingDown = true
        wifiRegistration?.unregister()
        wifiRegistration = null
        reconnectJob?.cancel()
        serviceScope.cancel()

        try {
            webSocket?.close(1000, "Service stopped")
            logError("Stopped websocket service")
        } catch (t: Throwable) {
            logError("webSocket.close() in onDestroy failed", t)
        } finally {
            webSocket = null
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaskerHA")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(text))
        } catch (t: Throwable) {
            logError("updateNotification failed", t)
        }
    }

    private fun connectWebSocket() {
        val url = HaSettings.resolveUrl(this)
        val token = HaSettings.loadToken(this)

        if (url.isBlank() || token.isBlank()) {
            logError("URL or token blank, stopping service")
            stopSelf()
            return
        }

        lastResolvedUrl = url
        updateNotification("Connecting to Home Assistant...")

        val wsUrl = url
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/api/websocket"

        if(!wsUrl.startsWith("ws://") && !wsUrl.startsWith("wss://")) {
            logError("Invalid URL: $wsUrl")
            return
        }

        logInfo("Connecting to $wsUrl")

        try {
            webSocket?.cancel()
        } catch (t: Throwable) {
            logError("webSocket.cancel() failed", t)
        }
        webSocket = null

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    logInfo("WebSocket opened")
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                    updateNotification("Authenticating with Home Assistant...")
                    webSocket.send("{\"type\": \"auth\", \"access_token\": \"$token\"}")
                } catch (t: Throwable) {
                    logError("onOpen crashed", t)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    logVerbose("onMessage: $text")

                    try {
                        val envelope = json.decodeFromString<OnSmallMessageEvent>(text)

                        when (envelope.type) {
                            "auth_ok" -> {
                                logInfo("Auth OK, subscribing to events")
                                updateNotification("Connected to Home Assistant")
                                reconnectAttempts = 0
                                reconnectJob?.cancel()
                                webSocket.send("""{"id":1,"type":"subscribe_events","event_type":"taskerha_message"}""")
                                // Reset counter to 1 (id=1 already used above); triggers get 2, 3, …
                                messageIdCounter.set(1)
                                wsSubIdToTriggerId.clear()
                                val (perIdGroups, legacyTriggers) = loadTriggerStateSubs()

                                if (perIdGroups.isEmpty() && legacyTriggers.isEmpty()) {
                                    logInfo("No TriggerStatePrefs stored; skipping subscribe_trigger")
                                } else {
                                    // One subscription per UUID group
                                    for ((triggerId, triggers) in perIdGroups) {
                                        val subId = messageIdCounter.incrementAndGet()
                                        val req = SubscribeTriggerRequest(
                                            type = "subscribe_trigger",
                                            id = subId,
                                            trigger = triggers
                                        )
                                        webSocket.send(payloadJson.encodeToString(req))
                                        wsSubIdToTriggerId[subId] = triggerId
                                        logInfo("Subscribed triggerId=$triggerId (wsId=$subId, ${triggers.size} entity/ies)")
                                    }
                                    // One batch subscription for all legacy (no UUID) triggers
                                    if (legacyTriggers.isNotEmpty()) {
                                        val subId = messageIdCounter.incrementAndGet()
                                        val req = SubscribeTriggerRequest(
                                            type = "subscribe_trigger",
                                            id = subId,
                                            trigger = legacyTriggers
                                        )
                                        webSocket.send(payloadJson.encodeToString(req))
                                        wsSubIdToTriggerId[subId] = "" // empty = legacy
                                        logInfo("Subscribed legacy batch (wsId=$subId, ${legacyTriggers.size} trigger(s))")
                                    }
                                }

                            }

                            "auth_invalid" -> {
                                logError("Auth invalid, stopping service")
                                stopSelf()
                            }

                            "event" -> {
                                try {
                                    val envelope = json.decodeFromString<HaMessageWsEnvelope>(text)
                                    when (envelope.type) {
                                        "event" -> {
                                            val ev = envelope.event ?: return
                                            if (ev.event_type == "taskerha_message") {
                                                logInfo("taskerha_message detected: type=${ev.data?.type}, message=${ev.data?.message}")
                                                this@HaWebSocketService.triggerOnHaMessageHelper2(
                                                    ev.data?.type,
                                                    ev.data?.message
                                                )
                                                return
                                            }
                                        }
                                    }
                                } catch (e2: Exception) {}

                                try {
                                    val envelope = json.decodeFromString<OnTriggerStateEnvelope>(text)
                                    when (envelope.type) {
                                        "event" -> {
                                            val ev = envelope.event ?: return
                                            if(!ev.variables.isEmpty() && ev.variables.containsKey("trigger")) {
                                                val trigger = ev.variables["trigger"] ?: return

                                                if(trigger.platform == "state"){
                                                    val wsId = envelope.id
                                                    if (wsId == null || !wsSubIdToTriggerId.containsKey(wsId)) {
                                                        logVerbose("Ignoring state event for unknown/stale wsId=$wsId")
                                                        return
                                                    }
                                                    val triggerId = wsSubIdToTriggerId[wsId]?.takeIf { it.isNotEmpty() }
                                                    logVerbose("State change detected: ${trigger.entity_id}, ${trigger.to_state.state}, triggerId=$triggerId")
                                                    val triggerJson = json.encodeToString(trigger)
                                                    this@HaWebSocketService.triggerOnTriggerStateEvent2(triggerJson, triggerId)
                                                }

                                            }
                                        }
                                    }
                                }catch (e2: Exception){
                                }

                            }
                        }
                    } catch (_: Exception) {

                    }
                } catch (t: Throwable) {
                    logError("onMessage crashed", t)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                try {
                    logError("onClosing: $code $reason")
                    runCatching { webSocket.close(code, reason) }
                        .onFailure { t -> logError("webSocket.close() failed", t) }

                    this@HaWebSocketService.webSocket = null
                    scheduleReconnect("onClosing $code $reason")
                } catch (t: Throwable) {
                    this@HaWebSocketService.webSocket = null
                    scheduleReconnect("onClosing callback crashed: ${t.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                try {
                    this@HaWebSocketService.webSocket = null
                    scheduleReconnect("Reschedule onFailure: ${t.message}")
                } catch (t2: Throwable) {
                    this@HaWebSocketService.webSocket = null
                    scheduleReconnect("Reschedule onFailure: ${t2.message}")
                }
            }
        })
    }

    /**
     * Called when WiFi connectivity changes. If the resolved URL has changed
     * (e.g. switched from home to remote or vice versa), force a reconnect.
     */
    private fun onWifiChanged() {
        if (isShuttingDown) return

        val newUrl = HaSettings.resolveUrl(this)
        if (newUrl == lastResolvedUrl) return

        logInfo("WiFi changed, URL switched from $lastResolvedUrl to $newUrl — reconnecting")
        reconnectAttempts = 0
        reconnectJob?.cancel()

        try {
            webSocket?.close(1000, "Network changed")
        } catch (_: Throwable) {}
        webSocket = null

        serviceScope.launch {
            delay(500) // small delay to let network settle
            runCatching { connectWebSocket() }
                .onFailure { t -> logError("connectWebSocket failed after WiFi change", t) }
        }
    }

    private fun scheduleReconnect(reason: String? = null) {
        if (isShuttingDown) {
            logInfo("Not scheduling reconnect, service shutting down")
            return
        }

        if (reconnectJob?.isActive == true) {
            logDebug("Reconnect already scheduled, skipping")
            return
        }

        val factor = 1 shl reconnectAttempts.coerceAtMost(10)
        val delayMs = (baseReconnectDelayMs * factor).coerceAtMost(maxReconnectDelayMs)
        reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(10)

        logInfo("Scheduling reconnect in ${delayMs}ms (attempt=$reconnectAttempts, reason=$reason)")
        updateNotification("Reconnecting to Home Assistant...")

        reconnectJob = serviceScope.launch {
            delay(delayMs)
            try {
                connectWebSocket()
            } catch (t: Throwable) {
                logError("connectWebSocket failed",t)
                scheduleReconnect("connectWebSocket crash: ${t.message}")
            }
        }
    }

    private val payloadJson = Json {
        encodeDefaults = true
        prettyPrint = false
    }
    private fun doResubscribeTriggers() {
        val ws = webSocket
        if (ws == null) {
            logInfo("doResubscribeTriggers: no active WebSocket, will subscribe on next connect")
            return
        }

        serviceScope.launch {
            try {
                // Clear active map — old zombie subscriptions on HA side will still fire events
                // but their IDs are no longer in the map, so they are silently ignored.
                wsSubIdToTriggerId.clear()

                val (perIdGroups, legacyTriggers) = loadTriggerStateSubs()
                if (perIdGroups.isEmpty() && legacyTriggers.isEmpty()) {
                    logInfo("doResubscribeTriggers: no triggers in prefs, not resubscribing")
                    return@launch
                }

                for ((triggerId, triggers) in perIdGroups) {
                    val subId = messageIdCounter.incrementAndGet()
                    val req = SubscribeTriggerRequest(
                        type = "subscribe_trigger",
                        id = subId,
                        trigger = triggers
                    )
                    ws.send(payloadJson.encodeToString(req))
                    wsSubIdToTriggerId[subId] = triggerId
                    logInfo("Resubscribed triggerId=$triggerId (wsId=$subId, ${triggers.size} entity/ies)")
                }

                if (legacyTriggers.isNotEmpty()) {
                    val subId = messageIdCounter.incrementAndGet()
                    val req = SubscribeTriggerRequest(
                        type = "subscribe_trigger",
                        id = subId,
                        trigger = legacyTriggers
                    )
                    ws.send(payloadJson.encodeToString(req))
                    wsSubIdToTriggerId[subId] = "" // empty = legacy
                    logInfo("Resubscribed legacy batch (wsId=$subId, ${legacyTriggers.size} trigger(s))")
                }

            } catch (t: Throwable) {
                logError("doResubscribeTriggers failed", t)
            }
        }
    }

    data class TriggerSubGroups(
        val perIdGroups: Map<String, List<StateTrigger>>,
        val legacyTriggers: List<StateTrigger>
    )

    private fun loadTriggerStateSubs(): TriggerSubGroups {
        val prefs = applicationContext.getSharedPreferences("TriggerStatePrefs", Context.MODE_PRIVATE)
        val items = prefs.getStringSet("items", emptySet()) ?: emptySet()

        val perIdGroups = mutableMapOf<String, MutableList<StateTrigger>>()
        val legacyTriggers = mutableListOf<StateTrigger>()

        for (raw in items) {
            runCatching {
                val built = payloadJson.decodeFromString<OnTriggerStateBuiltForm>(raw)

                val effectiveIds = if (built.entityIds.isNotEmpty()) {
                    built.entityIds.map { it.trim() }.filter { it.isNotBlank() }
                } else {
                    listOf(built.entityId.trim()).filter { it.isNotBlank() }
                }

                val stateTriggers = effectiveIds.map { entity ->
                    StateTrigger(
                        platform = "state",
                        entity_id = entity,
                        from = built.fromState.trim().takeIf { it.isNotBlank() },
                        to = built.toState.trim().takeIf { it.isNotBlank() },
                        for_ = parseForDuration(built.forDuration)
                    )
                }

                val tid = built.triggerId
                if (tid != null) {
                    perIdGroups.getOrPut(tid) { mutableListOf() }.addAll(stateTriggers)
                } else {
                    legacyTriggers.addAll(stateTriggers)
                }
            }
        }

        return TriggerSubGroups(perIdGroups, legacyTriggers)
    }

    private fun parseForDuration(forDuration: String): HaForDuration {
        val v = forDuration.trim()
        if (v.isBlank()) return HaForDuration()

        val parts = v.split(":")
        val h = parts.getOrNull(0)?.trim().orEmpty()
        val m = parts.getOrNull(1)?.trim().orEmpty()
        val s = parts.getOrNull(2)?.trim().orEmpty()

        // treat "0:0:0" / "0" / etc as no duration if you want
        val hh = h.toLongOrNull() ?: 0L
        val mm = m.toLongOrNull() ?: 0L
        val ss = s.toLongOrNull() ?: 0L

        if (hh == 0L && mm == 0L && ss == 0L) return HaForDuration()
        return HaForDuration(hours = hh, minutes = mm, seconds = ss)
    }
}
