package com.github.db1996.taskerha.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.db1996.taskerha.R
import com.github.db1996.taskerha.datamodels.HaSettings
import com.github.db1996.taskerha.service.data.HaWsEnvelope
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("Connecting to Home Assistant..."))
        connectWebSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        webSocket?.close(1000, "Service stopped")
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TaskerHA")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setOngoing(true)
            .build()
    }

    private fun connectWebSocket() {
        val url = HaSettings.loadUrl(this)
        val token = HaSettings.loadToken(this)

        if (url.isBlank() || token.isBlank()) {
            stopSelf()
            return
        }

        // Convert REST URL to WS URL (basic version, adapt to your setup)
        val wsUrl = url
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/api/websocket"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: okhttp3.Response) {
                ws.send("{\"type\": \"auth\", \"access_token\": \"$token\"}")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                Log.d("HaWebSocketService", "onMessage: $text")

                val envelope = try {
                    json.decodeFromString<HaWsEnvelope>(text)
                } catch (e: Exception) {
                    Log.e("HaWebSocketService", "Invalid WS JSON: ${e.message}")
                    return
                }

                when (envelope.type) {
                    "auth_ok" -> ws.send("""{"id":$TRIGGER_STATE_EVENT_ID,"type":"subscribe_events","event_type":"state_changed"}""")
                    "auth_invalid" -> {
                        Log.e("HaWebSocketService", "Auth invalid")
                        stopSelf()
                    }
                    "event" -> {
                        val ev = envelope.event ?: return
                        if (ev.event_type == "state_changed") {
                            val data = ev.data ?: return
                            Log.d(
                                "HaWebSocketService",
                                "state_changed for ${data.entity_id}: ${data.old_state?.state} -> ${data.new_state?.state}"
                            )
                            this@HaWebSocketService.triggerOnTriggerStateEvent(text)
                        }
                    }
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.e("HaWebSocketService", "onClosing: $code $reason")
                ws.close(code, reason)
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Log.e("HaWebSocketService", "onFailure: ${t.message}")
                stopSelf()
            }
        })
    }
}
