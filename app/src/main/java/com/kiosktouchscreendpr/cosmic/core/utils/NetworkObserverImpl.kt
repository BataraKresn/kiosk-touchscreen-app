package com.kiosktouchscreendpr.cosmic.core.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

interface NetworkObserver {
    val isConnected: StateFlow<Boolean>
    fun register()
    fun unregister()
}

class NetworkObserverImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isConnected = MutableStateFlow(checkIfCurrentlyConnected())
    override val isConnected: StateFlow<Boolean> = _isConnected

    private fun checkIfCurrentlyConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val hasInternet =
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (hasInternet) _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }

        override fun onUnavailable() {
            _isConnected.value = false
        }
    }

    override fun register() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        println("NetworkObserver registered")
    }

    override fun unregister() {
        try {
            connectivityManager.unregisterNetworkCallback(callback)
            println("NetworkObserver unregistered")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}