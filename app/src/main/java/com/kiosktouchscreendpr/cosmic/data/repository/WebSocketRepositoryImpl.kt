package com.kiosktouchscreendpr.cosmic.data.repository

import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.Message
import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.WebSocketDataSource
import com.kiosktouchscreendpr.cosmic.domain.repository.WebSocketRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class WebSocketRepositoryImpl @Inject constructor(
    private val datasource: WebSocketDataSource
) : WebSocketRepository {
    override suspend fun connect(url: String) = datasource.connect(url)

    override suspend fun disconnect() = datasource.disconnect()

    override suspend fun send(message: Message) = datasource.send(message)

    override fun observeMessages(): Flow<Message> = datasource.messageFlow

    override fun isConnected(): Boolean = datasource.isConnected()
}