package com.kiosktouchscreendpr.cosmic.domain.usecase

import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.Message
import com.kiosktouchscreendpr.cosmic.domain.repository.WebSocketRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class WebSocketUseCase @Inject constructor(
    private val repository: WebSocketRepository,
) {

    suspend fun connect(url: String) = repository.connect(url)

    suspend fun disconnect() = repository.disconnect()

    fun observeMessages(): Flow<Message> = repository.observeMessages()

    fun isConnected(): Boolean = repository.isConnected()

}