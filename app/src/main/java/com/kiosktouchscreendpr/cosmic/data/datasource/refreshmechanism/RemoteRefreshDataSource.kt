package com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism

import kotlinx.coroutines.flow.Flow

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface RemoteRefreshDataSource {
    val refreshFlow: Flow<RefreshRes>
    suspend fun connect(url: String)
    suspend fun disconnect()
}