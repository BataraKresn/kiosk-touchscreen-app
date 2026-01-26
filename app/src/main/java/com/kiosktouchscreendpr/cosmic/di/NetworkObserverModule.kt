package com.kiosktouchscreendpr.cosmic.di

import com.kiosktouchscreendpr.cosmic.core.utils.ConnectivityObserver
import com.kiosktouchscreendpr.cosmic.core.utils.ConnectivityObserverImpl
import com.kiosktouchscreendpr.cosmic.core.utils.NetworkObserver
import com.kiosktouchscreendpr.cosmic.core.utils.NetworkObserverImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkObserverModule {

    @Binds
    abstract fun bindNetworkObserver(
        networkObserverImpl: NetworkObserverImpl
    ): NetworkObserver

    @Binds
    abstract fun bindConnectivityObserver(
        connectivityObserverImpl: ConnectivityObserverImpl
    ): ConnectivityObserver

}