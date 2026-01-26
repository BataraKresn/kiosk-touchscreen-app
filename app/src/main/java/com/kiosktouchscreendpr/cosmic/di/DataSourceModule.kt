package com.kiosktouchscreendpr.cosmic.di

import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.WebSocketDataSource
import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.WebSocketDataSourceImpl
import com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism.RemoteRefreshDataSource
import com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism.RemoteRefreshDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindWebSocketDatasource(
        webSocketDataSourceImpl: WebSocketDataSourceImpl
    ): WebSocketDataSource

    @Binds
    @Singleton
    abstract fun bindRemoteRefresh(
        remoteRefreshImpl: RemoteRefreshDataSourceImpl
    ) : RemoteRefreshDataSource
}