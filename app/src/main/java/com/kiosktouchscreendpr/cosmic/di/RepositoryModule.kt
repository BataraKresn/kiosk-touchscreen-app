package com.kiosktouchscreendpr.cosmic.di

import com.kiosktouchscreendpr.cosmic.data.repository.RemoteRefreshRepositoryImpl
import com.kiosktouchscreendpr.cosmic.data.repository.WebSocketRepositoryImpl
import com.kiosktouchscreendpr.cosmic.domain.repository.RemoteRefreshRepository
import com.kiosktouchscreendpr.cosmic.domain.repository.WebSocketRepository
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
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWebSocketRepository(
        impl: WebSocketRepositoryImpl
    ): WebSocketRepository

    @Binds
    @Singleton
    abstract fun bindRemoteRefreshRepository(
       impl: RemoteRefreshRepositoryImpl
    ): RemoteRefreshRepository
}