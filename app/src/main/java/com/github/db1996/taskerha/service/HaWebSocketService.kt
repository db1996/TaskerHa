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
import com.github.db1996.taskerha.service.data.HaMessageWsEnvelope
import com.github.db1996.taskerha.service.data.OnTriggerStateWsEnvelope
import com.github.db1996.taskerha.tasker.base.BaseLogger
import com.github.db1996.taskerha.tasker.onHaMessage.triggerOnHaMessageHelper2
import com.github.db1996.taskerha.tasker.ontriggerstate.triggerOnTriggerStateEvent2
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val TRIGGER_STATE_EVENT_ID = 1

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
                        val envelope = json.decodeFromString<OnTriggerStateWsEnvelope>(text)

                        when (envelope.type) {
                            "auth_ok" -> {
                                logInfo("Auth OK, subscribing to events")
                                updateNotification("Connected to Home Assistant")
                                reconnectAttempts = 0
                                reconnectJob?.cancel()

                                webSocket.send(
                                    """{"id":$TRIGGER_STATE_EVENT_ID,"type":"subscribe_events","event_type":"state_changed"}"""
                                )
                                webSocket.send(
                                    """{"id":2,"type":"subscribe_events","event_type":"taskerha_message"}"""
                                )
                            }

                            "auth_invalid" -> {
                                logError("Auth invalid, stopping service")
                                stopSelf()
                            }

                            "event" -> {
                                val ev = envelope.event ?: return
                                if (ev.event_type == "state_changed") {
                                    logVerbose("State changed: ${ev.data?.entity_id}, ${ev.data?.new_state?.state}")
                                    this@HaWebSocketService.triggerOnTriggerStateEvent2(text)
                                }
                            }
                        }
                    } catch (_: Exception) {
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
                                    }
                                }
                            }
                        } catch (e2: Exception) {
                            logVerbose("Not HaMessage: ${e2.message}")
                            return
                        }
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
                    logError("onClosing callback crashed", t)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                try {
                    logError("onFailure: ${t.message}", t)
                    this@HaWebSocketService.webSocket = null
                } catch (t2: Throwable) {
                    logError("onFailure callback crashed", t)
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
}
