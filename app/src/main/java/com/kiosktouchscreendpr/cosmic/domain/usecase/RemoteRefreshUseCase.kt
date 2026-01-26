package com.kiosktouchscreendpr.cosmic.domain.usecase

import com.kiosktouchscreendpr.cosmic.domain.repository.RemoteRefreshRepository
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class RemoteRefreshUseCase @Inject constructor(
    private val repository: RemoteRefreshRepository
) {
    suspend fun connect(url: String) = repository.connect(url)

    suspend fun disconnect() = repository.disconnect()

    fun observeMessages() = repository.observeMessages()

}