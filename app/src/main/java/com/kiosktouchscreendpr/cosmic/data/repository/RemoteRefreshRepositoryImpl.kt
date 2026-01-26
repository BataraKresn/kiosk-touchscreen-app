package com.kiosktouchscreendpr.cosmic.data.repository

import com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism.*
import com.kiosktouchscreendpr.cosmic.domain.repository.RemoteRefreshRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
class RemoteRefreshRepositoryImpl @Inject constructor(
    private val dataSource: RemoteRefreshDataSource
) : RemoteRefreshRepository {
    override suspend fun connect(url: String) = dataSource.connect(url)

    override suspend fun disconnect() = dataSource.disconnect()

    override fun observeMessages(): Flow<RefreshRes> = dataSource.refreshFlow
}