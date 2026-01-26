package com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class RemoteRefreshDataSourceImpl @Inject constructor(
    private val client: HttpClient,
) : RemoteRefreshDataSource {

    private var sesh: WebSocketSession? = null
    private val _refreshFlow = MutableSharedFlow<RefreshRes>()
    override val refreshFlow = _refreshFlow.asSharedFlow()


    override suspend fun connect(url: String) {
        try {
            client.webSocket(url) {
                sesh = this
                while (isActive) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            _refreshFlow.emit(RefreshRes.Triggered(text))
                        }

                        else -> {
                            /* leave it empty dude */
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _refreshFlow.emit(RefreshRes.Error("Connection error: ${e.message}"))
        }
    }

    override suspend fun disconnect() {
        try {
            sesh?.close()
            sesh = null
        } catch (e: Exception) {
            _refreshFlow.emit(RefreshRes.Error("Disconnection error: ${e.message}"))
        }
    }
}