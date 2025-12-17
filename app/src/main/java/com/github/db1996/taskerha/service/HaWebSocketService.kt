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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger


class HaWebSocketService : Service(), BaseLogger {
    private val json = Json { ignoreUnknownKeys = true }
    private val TRIGGER_STATE_EVENT_ID = 2

    override val logTag: String
        get() = "HaWebSocketService"

    override val logChannel: LogChannel
        get() = LogChannel.WEBSOCKET

    companion object {
        const val TAG = "HaWebSocketService"
        const val CHANNEL_ID = "ha_websocket_channel"
        const val NOTIFICATION_ID = 1001

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
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        logError("coroutine crashed", t)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    private var webSocket: WebSocket? = null

    private val httpClient: OkHttpClient by lazy {
        Logger.getLogger(OkHttpClient::class.java.getName()).setLevel(Level.FINE)
        OkHttpClient.Builder()
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    // === Reconnect state ===
    private val baseReconnectDelayMs = 1_000L  // 1 second
    private val maxReconnectDelayMs = 60_000L  // 60 seconds
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    @Volatile
    private var isShuttingDown = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Connecting to Home Assistant..."))

        if (!isShuttingDown && webSocket == null && reconnectJob?.isActive != true) {
            logInfo("Starting websocket service from onStartCommand")
            runCatching { connectWebSocket() }
                .onFailure { t -> logError("connectWebSocket failed from onStartCommand", t) }
        }

        return START_STICKY
    }


    override fun onCreate() {
        super.onCreate()

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Connecting to Home Assistant...")
        )

        logInfo("Starting websocket service from onCreate")

        runCatching { connectWebSocket() }
            .onFailure { t -> logError("connectWebSocket failed", t) }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShuttingDown = true
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
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)

        if (url.isBlank() || token.isBlank()) {
            logError("URL or token blank, stopping service")
            stopSelf()
            return
        }

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
//                                val testeventSub = "{\n" +
//                                        "    \"id\": 1,\n" +
//                                        "    \"type\": \"subscribe_trigger\",\n" +
//                                        "    \"trigger\": {\n" +
//                                        "        \"platform\": \"state\",\n" +
//                                        "        \"entity_id\": \"light.pc_kamer_2\"\n" +
//                                        "    }\n" +
//                                        "}";
//                                logInfo(testeventSub)
//
//                                webSocket.send(testeventSub)

                                val triggers = loadTriggerStateSubs()

                                if (triggers.isEmpty()) {
                                    logInfo("No TriggerStatePrefs stored; skipping subscribe_trigger")
                                } else {
                                    val req = SubscribeTriggerRequest(
                                        type = "subscribe_trigger",
                                        id = TRIGGER_STATE_EVENT_ID,
                                        trigger = triggers
                                    )
                                    val body = payloadJson.encodeToString(req)
                                    logInfo("Subscribing to ${triggers.size} state trigger(s)")
                                    logInfo(json.encodeToString(req))
                                    webSocket.send(body)
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
                                                    logVerbose("State change detected: ${trigger.entity_id}, ${trigger.to_state.state}")
                                                    val triggerJson = json.encodeToString(trigger)
                                                    this@HaWebSocketService.triggerOnTriggerStateEvent2(triggerJson)
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
    private fun loadTriggerStateSubs(): List<StateTrigger> {
        val prefs = applicationContext.getSharedPreferences("TriggerStatePrefs", Context.MODE_PRIVATE)
        val items = prefs.getStringSet("items", emptySet()) ?: emptySet()

        return items.mapNotNull { raw ->
            runCatching {
                val built = payloadJson.decodeFromString<OnTriggerStateBuiltForm>(raw)

                val entity = built.entityId.trim()
                if (entity.isBlank()) return@runCatching null

                StateTrigger(
                    platform = "state",
                    entity_id = entity,
                    from = built.fromState.trim().takeIf { it.isNotBlank() },
                    to = built.toState.trim().takeIf { it.isNotBlank() },
                    for_ = parseForDuration(built.forDuration)
                )
            }.getOrNull()
        }
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
