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
import com.github.db1996.taskerha.service.data.HaMessageWsEnvelope
import com.github.db1996.taskerha.service.data.OnTriggerStateWsEnvelope
import com.github.db1996.taskerha.tasker.onHaMessage.triggerOnHaMessageHelper
import com.github.db1996.taskerha.tasker.ontriggerstate.triggerOnTriggerStateEvent
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class HaWebSocketService : Service() {

    private val json = Json { ignoreUnknownKeys = true }

    private val TRIGGER_STATE_EVENT_ID = 1

    companion object {
        const val CHANNEL_ID = "ha_websocket_channel"
        const val NOTIFICATION_ID = 1001

        @RequiresApi(Build.VERSION_CODES.O)
        fun start(context: Context) {
            val intent = Intent(context, HaWebSocketService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, HaWebSocketService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var webSocket: WebSocket? = null
    private val httpClient = OkHttpClient()

    // === Reconnect state ===
    private val baseReconnectDelayMs = 1_000L       // 1 second
    private val maxReconnectDelayMs  = 60_000L      // 60 seconds
    private var reconnectAttempts    = 0
    private var reconnectJob: Job?   = null
    @Volatile
    private var isShuttingDown       = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            buildNotification("Connecting to Home Assistant...")
        )
        connectWebSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        isShuttingDown = true
        reconnectJob?.cancel()
        serviceScope.cancel()
        webSocket?.close(1000, "Service stopped")
        webSocket = null
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
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun connectWebSocket() {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)

        if (url.isBlank() || token.isBlank()) {
            Log.e("HaWebSocketService", "URL or token blank, stopping service")
            stopSelf()
            return
        }

        updateNotification("Connecting to Home Assistant...")

        val wsUrl = url
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/api/websocket"

        webSocket?.cancel()
        webSocket = null

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                Log.d("HaWebSocketService", "WebSocket opened")
                reconnectAttempts = 0
                reconnectJob?.cancel()
                updateNotification("Authenticating with Home Assistant...")
                webSocket.send("{\"type\": \"auth\", \"access_token\": \"$token\"}")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("HaWebSocketService", "onMessage: $text")

                // OnTriggerState
                try {
                    val envelope = json.decodeFromString<OnTriggerStateWsEnvelope>(text)

                    when (envelope.type) {
                        "auth_ok" -> {
                            Log.d("HaWebSocketService", "Auth OK, subscribing to state_changed")
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
                            Log.e("HaWebSocketService", "Auth invalid, stopping service")
                            stopSelf()
                        }
                        "event" -> {
                            val ev = envelope.event ?: return
                            if (ev.event_type == "state_changed") {
                                this@HaWebSocketService.triggerOnTriggerStateEvent(text)
                            }
                        }
                    }
                } catch (e: Exception) {

                    try {
                        val envelope = json.decodeFromString<HaMessageWsEnvelope>(text)

                        when (envelope.type) {
                            "event" -> {
                                val ev = envelope.event ?: return

                                if(ev.event_type == "taskerha_message"){
                                    Log.d("HaWebSocketService", "HaMessage: ${ev.data?.type}, ${ev.data?.message}")

                                    this@HaWebSocketService.triggerOnHaMessageHelper(ev.data?.type,
                                        ev.data?.message
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("HaWebSocketService", "Not HaMessage: ${e.message}")
                        return
                    }
                }

            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.e("HaWebSocketService", "onClosing: $code $reason")
                webSocket.close(code, reason)
                this@HaWebSocketService.webSocket = null

                scheduleReconnect("onClosing $code $reason")
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: okhttp3.Response?
            ) {
                Log.e("HaWebSocketService", "onFailure: ${t.message}")
                this@HaWebSocketService.webSocket = null

                scheduleReconnect("onFailure: ${t.message}")
            }
        })
    }

    private fun scheduleReconnect(reason: String? = null) {
        if (isShuttingDown) {
            Log.d("HaWebSocketService", "Not scheduling reconnect, service shutting down")
            return
        }

        if (reconnectJob?.isActive == true) {
            Log.d("HaWebSocketService", "Reconnect already scheduled, skipping")
            return
        }

        val factor = 1 shl reconnectAttempts.coerceAtMost(10)
        val delayMs = (baseReconnectDelayMs * factor).coerceAtMost(maxReconnectDelayMs)

        reconnectAttempts = (reconnectAttempts + 1).coerceAtMost(10)

        Log.d(
            "HaWebSocketService",
            "Scheduling reconnect in ${delayMs}ms (attempt=$reconnectAttempts, reason=$reason)"
        )

        updateNotification("Reconnecting to Home Assistant...")

        reconnectJob = serviceScope.launch {
            delay(delayMs)
            connectWebSocket()
        }
    }
}
