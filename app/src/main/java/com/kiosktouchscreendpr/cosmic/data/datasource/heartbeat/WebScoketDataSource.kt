package com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat

import kotlinx.coroutines.flow.SharedFlow

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface WebSocketDataSource {
    val messageFlow: SharedFlow<Message>
    suspend fun connect(url: String)
    suspend fun disconnect()
    suspend fun send(message: Message)
    fun isConnected(): Boolean
}