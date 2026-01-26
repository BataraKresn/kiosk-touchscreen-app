package com.kiosktouchscreendpr.cosmic.domain.repository

import com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism.RefreshRes
import kotlinx.coroutines.flow.Flow

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface RemoteRefreshRepository {
    suspend fun connect(url: String)
    suspend fun disconnect()
    fun observeMessages(): Flow<RefreshRes>
}