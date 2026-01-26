package com.kiosktouchscreendpr.cosmic.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */
interface ConnectivityObserver {
    val isConnected: Flow<Boolean>
}
class ConnectivityObserverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectivityObserver {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!

    private val _isConnected = MutableStateFlow(checkInitialConnectionState())
    override val isConnected: Flow<Boolean> = _isConnected.asStateFlow()

    private fun checkInitialConnectionState(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false
    }

    init {
        val callback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val connected = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                _isConnected.value = connected
            }

            override fun onUnavailable() {
                super.onUnavailable()
                _isConnected.value = false
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                _isConnected.value = false
            }

            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                _isConnected.value = true
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
    }
}