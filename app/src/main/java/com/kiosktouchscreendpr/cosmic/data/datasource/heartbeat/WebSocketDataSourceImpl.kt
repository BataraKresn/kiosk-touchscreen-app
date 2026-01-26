package com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat

import android.util.Log
import com.kiosktouchscreendpr.cosmic.core.utils.formatLink
import com.kiosktouchscreendpr.cosmic.core.utils.getDeviceIP
import com.kiosktouchscreendpr.cosmic.data.dto.MessageDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class WebSocketDataSourceImpl @Inject constructor(
    private val client: HttpClient,
    private val json: Json,
    private val scope: CoroutineScope
) : WebSocketDataSource {

    private var sesh: WebSocketSession? = null
    private val _messagesFlow = MutableSharedFlow<Message>()
    override val messageFlow = _messagesFlow.asSharedFlow()

    private var heartbeatJob: Job? = null
    private var lastHeartbeatResponse: Long = 0
    private var heartbeatInterval = 15_000L
    private var heartbeatTimeout = 45_000L

    override suspend fun connect(url: String) {
        try {
            client.webSocket(url) {
                sesh = this

                println("connected to $url")
                startHeartbeat()

                while (isActive) {
                    val frame = incoming.receive()

                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()

                            if (text == "pong") {
                                lastHeartbeatResponse = System.currentTimeMillis()
                            } else {
                                _messagesFlow.emit(Message.Text(text, "server"))
                            }
                        }

                        is Frame.Binary -> {
                            val data = frame.data
                            _messagesFlow.emit(Message.Binary(data))
                        }

                        else -> { /* leave it empty dude */
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _messagesFlow.emit(Message.Error("Connection error: ${e.message}"))
        } finally {
            stopHeartbeat()
        }
    }

    override suspend fun disconnect() {
        stopHeartbeat()
        try {
            sesh?.close()
            sesh = null
            println("Disconnected")
        } catch (e: Exception) {
            _messagesFlow.emit(Message.Error("Disconnection error: ${e.message}"))
        }
    }

    override suspend fun send(message: Message) {
        val currentSesh = sesh
        if (currentSesh != null && currentSesh.isActive) {
            when (message) {

                is Message.HeartBeat -> {
                    val message = MessageDto(
                        token = message.token,
                        isActive = message.isActive,
                        message = message.message
                    )

                    val jsonMessage = json.encodeToString(message)

                    currentSesh.send(Frame.Text(jsonMessage))

                }

                else -> {
                    /* nah leave it empty frfr */
                }
            }
        }
    }

    override fun isConnected(): Boolean = sesh?.isActive == true


    private fun startHeartbeat() {
        stopHeartbeat()

        Log.d("Heartbeat", "Starting heartbeat")
        heartbeatJob = scope.launch {
            while (isActive) {
                if (isConnected()) {
                    sendHeartbeat()

                    val currentTime = System.currentTimeMillis()
                    if (lastHeartbeatResponse > 0 &&
                        currentTime - lastHeartbeatResponse > heartbeatTimeout
                    ) {
                        _messagesFlow.emit(Message.Error("Heartbeat timeout - connection may be lost"))
                        // could implement automatic reconnection logic here
                    }
                }

                delay(heartbeatInterval)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun sendHeartbeat() {

        val getDeviceIP = getDeviceIP()

        val heartbeat = Message.HeartBeat(
            token = formatLink(getDeviceIP ?: "0.0.0.0"),
            isActive = isAppActive(),
            message = "Device is Active"
        )

        println("Sending heartbeat: $heartbeat")
        send(heartbeat)
    }

    private fun isAppActive(): Boolean {
        // this would be implemented with actual platform-specific code
        // tor example, on Android you would use ProcessLifecycleOwner
        return true // Default to active for now
    }
}