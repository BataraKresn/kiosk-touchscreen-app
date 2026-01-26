package com.kiosktouchscreendpr.cosmic.domain.repository

import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.Message
import kotlinx.coroutines.flow.Flow

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface WebSocketRepository {
    suspend fun connect(url: String)
    suspend fun disconnect()
    suspend fun send(message: Message)
    fun observeMessages(): Flow<Message>
    fun isConnected(): Boolean
}