package com.github.db1996.taskerha.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.github.db1996.taskerha.R
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.logging.CustomLogger
import com.github.db1996.taskerha.logging.LogChannel
import com.github.db1996.taskerha.service.data.HaMessageWsEnvelope
import com.github.db1996.taskerha.service.data.OnTriggerStateWsEnvelope
import com.github.db1996.taskerha.tasker.onHaMessage.triggerOnHaMessageHelper
import com.github.db1996.taskerha.tasker.ontriggerstate.triggerOnTriggerStateEvent
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

class HaWebSocketService : Service() {

    private val TAG = "HaWebSocketService"

    private val json = Json { ignoreUnknownKeys = true }
    private val TRIGGER_STATE_EVENT_ID = 1

    companion object {
        const val CHANNEL_ID = "ha_websocket_channel"
        const val NOTIFICATION_ID = 1001

        @RequiresApi(Build.VERSION_CODES.O)
        fun start(context: Context) {
            Log.e("HaWebSocketService", "start called")
            val intent = Intent(context, HaWebSocketService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HaWebSocketService::class.java)
            context.stopService(intent)
        }
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        CustomLogger.e(TAG, "coroutine crashed", LogChannel.WEBSOCKET, t)
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler)

    private var webSocket: WebSocket? = null

    private val httpClient: OkHttpClient by lazy {
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

    override fun onCreate() {
        super.onCreate()

        startForeground(
            NOTIFICATION_ID,
            buildNotification("Connecting to Home Assistant...")
        )

        runCatching { connectWebSocket() }
            .onFailure { t -> CustomLogger.e(TAG, "connectWebSocket failed", LogChannel.WEBSOCKET, t) }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShuttingDown = true
        reconnectJob?.cancel()
        serviceScope.cancel()

        try {
            webSocket?.close(1000, "Service stopped")
        } catch (t: Throwable) {
            CustomLogger.e(TAG, "webSocket.close() in onDestroy failed", LogChannel.WEBSOCKET, t)
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
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(text))
        } catch (t: Throwable) {
            CustomLogger.e(TAG, "updateNotification crashed", LogChannel.WEBSOCKET, t)
        }
    }

    private fun connectWebSocket() {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)

        if (url.isBlank() || token.isBlank()) {
            CustomLogger.e(TAG, "URL or token blank, stopping service", LogChannel.WEBSOCKET)
            stopSelf()
            return
        }

        updateNotification("Connecting to Home Assistant...")

        val wsUrl = url
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/api/websocket"

        try {
            webSocket?.cancel()
        } catch (t: Throwable) {
            CustomLogger.e(TAG, "webSocket.cancel() failed", LogChannel.WEBSOCKET, t)
        }
        webSocket = null

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                try {
                    CustomLogger.i(TAG, "WebSocket opened", LogChannel.WEBSOCKET)
                    reconnectAttempts = 0
                    reconnectJob?.cancel()
                    updateNotification("Authenticating with Home Assistant...")
                    webSocket.send("{\"type\": \"auth\", \"access_token\": \"$token\"}")
                } catch (t: Throwable) {
                    CustomLogger.e(TAG, "onOpen crashed", LogChannel.WEBSOCKET, t)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    CustomLogger.v(TAG, "onMessage: $text", LogChannel.WEBSOCKET)

                    try {
                        val envelope = json.decodeFromString<OnTriggerStateWsEnvelope>(text)

                        when (envelope.type) {
                            "auth_ok" -> {
                                CustomLogger.i(TAG, "Auth OK, subscribing to events", LogChannel.WEBSOCKET)
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
                                CustomLogger.e(TAG, "Auth invalid, stopping service", LogChannel.WEBSOCKET)
                                stopSelf()
                            }

                            "event" -> {
                                val ev = envelope.event ?: return
                                if (ev.event_type == "state_changed") {
                                    CustomLogger.v(TAG, "State changed: ${ev.data?.entity_id}, ${ev.data?.new_state?.state}", LogChannel.WEBSOCKET)
                                    this@HaWebSocketService.triggerOnTriggerStateEvent(text)
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
                                        CustomLogger.i(TAG, "TaskerHaMessage: ${ev.data?.type}, ${ev.data?.message}", LogChannel.WEBSOCKET)

                                        this@HaWebSocketService.triggerOnHaMessageHelper(
                                            ev.data?.type,
                                            ev.data?.message
                                        )
                                    }
                                }
                            }
                        } catch (e2: Exception) {
                            CustomLogger.v(TAG, "Not HaMessage: ${e2.message}", LogChannel.WEBSOCKET)
                            return
                        }
                    }
                } catch (t: Throwable) {
                    CustomLogger.e(TAG, "onMessage crashed", LogChannel.WEBSOCKET, t)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                try {
                    CustomLogger.e(TAG, "onClosing: $code $reason")
                    runCatching { webSocket.close(code, reason) }
                        .onFailure { t -> CustomLogger.e(TAG, "webSocket.close() failed", LogChannel.WEBSOCKET, t) }

                    this@HaWebSocketService.webSocket = null
                    scheduleReconnect("onClosing $code $reason")
                } catch (t: Throwable) {
                    CustomLogger.e(TAG, "onClosing callback crashed", LogChannel.WEBSOCKET, t)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                try {
                    CustomLogger.e(TAG, "onFailure: ${t.message}", LogChannel.WEBSOCKET, t)
                    this@HaWebSocketService.webSocket = null
                    scheduleReconnect("onFailure: ${t.message}")
                } catch (t2: Throwable) {
                    CustomLogger.e(TAG, "onFailure callback crashed", LogChannel.WEBSOCKET, t2)
                }
            }
        })
    }

    private fun scheduleReconnect(reason: String? = null) {
        if (isShuttingDown) {
            CustomLogger.d(TAG, "Not scheduling reconnect, service shutting down")
            return
        }

        if (reconnectJob?.isActive == true) {
            CustomLogger.d(TAG, "Reconnect already scheduled, skipping")
            return
        }

        val factor = 1 shl reconnectAttempts.coerceAtMost(10)
        val delayMs = (baseReconnectDelayMs * factor).coerceAtMost(maxReconnectDelayMs)
        reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(10)

        CustomLogger.i(TAG, "Scheduling reconnect in ${delayMs}ms (attempt=$reconnectAttempts, reason=$reason)")
        updateNotification("Reconnecting to Home Assistant...")

        reconnectJob = serviceScope.launch {
            delay(delayMs)
            try {
                connectWebSocket()
            } catch (t: Throwable) {
                CustomLogger.e(TAG, "connectWebSocket failed", LogChannel.WEBSOCKET, t)
                // If connect itself is crashing, try again later instead of dying.
                scheduleReconnect("connectWebSocket crash: ${t.message}")
            }
        }
    }
}
