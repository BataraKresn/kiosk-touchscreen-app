package com.kiosktouchscreendpr.cosmic.core.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FIX #4: Network Observer with Debouncing and Stability Window
 * 
 * Key Improvements:
 * 1. Network events are debounced (no immediate reactions to flapping)
 * 2. Stability window ensures network is consistently available
 * 3. Network state is a SIGNAL, not a COMMAND
 * 4. No direct reconnect triggers - delegates to ConnectionManager
 * 5. Tracks network quality (WiFi vs Cellular, signal strength)
 * 
 * This observer NEVER triggers reconnection directly.
 * It only reports network availability to ConnectionManager,
 * which makes the final decision.
 * 
 * @author Cosmic Development Team (Anti-Flapping Edition)
 */
@Singleton
class NetworkObserver @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NetworkObserver"
        private const val DEBOUNCE_MS = 2000L // 2s debounce
        private const val STABILITY_WINDOW_MS = 3000L // 3s stability requirement
    }

    data class NetworkStatus(
        val isAvailable: Boolean,
        val isValidated: Boolean,
        val isMetered: Boolean,
        val transportType: TransportType,
        val signalStrength: Int? = null, // 0-100, if available
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class TransportType {
            NONE, WIFI, CELLULAR, ETHERNET, BLUETOOTH, VPN
        }
        
        val isStable: Boolean
            get() = isAvailable && isValidated
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkStatus = MutableStateFlow(getCurrentNetworkStatus())
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _isStable = MutableStateFlow(false)
    val isStable: StateFlow<Boolean> = _isStable.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null
    private var stabilityJob: Job? = null

    private var lastAvailableTime = 0L
    private var lastLostTime = 0L

    init {
        registerNetworkCallback()
        startStabilityMonitor()
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                lastAvailableTime = System.currentTimeMillis()
                debounceNetworkChange(true)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val status = parseNetworkCapabilities(network, networkCapabilities)
                Log.d(TAG, "Capabilities changed: ${status.transportType}, validated=${status.isValidated}")
                
                _networkStatus.value = status
                
                if (status.isStable) {
                    debounceNetworkChange(true)
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                lastLostTime = System.currentTimeMillis()
                debounceNetworkChange(false)
            }

            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                debounceNetworkChange(false)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                Log.w(TAG, "Network losing in ${maxMsToLive}ms: $network")
                // Pre-emptive warning, but don't act yet
            }
        }

        connectivityManager.registerNetworkCallback(request, callback)
        Log.d(TAG, "Network callback registered")
    }

    /**
     * Debounce network changes to avoid flapping
     */
    private fun debounceNetworkChange(available: Boolean) {
        debounceJob?.cancel()
        
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            
            // Re-check actual status after debounce
            val currentStatus = getCurrentNetworkStatus()
            _networkStatus.value = currentStatus
            
            Log.d(TAG, "Debounced network change: available=$available, validated=${currentStatus.isValidated}")
        }
    }

    /**
     * Monitor network stability over time
     */
    private fun startStabilityMonitor() {
        scope.launch {
            networkStatus
                .map { it.isStable }
                .distinctUntilChanged()
                .collect { stable ->
                    stabilityJob?.cancel()
                    
                    if (stable) {
                        // Network appears stable, start countdown
                        stabilityJob = launch {
                            delay(STABILITY_WINDOW_MS)
                            
                            // Re-check after window
                            if (networkStatus.value.isStable) {
                                _isStable.value = true
                                Log.i(TAG, "✅ Network confirmed stable")
                            }
                        }
                    } else {
                        _isStable.value = false
                        Log.w(TAG, "❌ Network unstable")
                    }
                }
        }
    }

    /**
     * Get current network status
     */
    private fun getCurrentNetworkStatus(): NetworkStatus {
        val network = connectivityManager.activeNetwork
        
        if (network == null) {
            return NetworkStatus(
                isAvailable = false,
                isValidated = false,
                isMetered = false,
                transportType = NetworkStatus.TransportType.NONE
            )
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return if (capabilities != null) {
            parseNetworkCapabilities(network, capabilities)
        } else {
            NetworkStatus(
                isAvailable = true,
                isValidated = false,
                isMetered = false,
                transportType = NetworkStatus.TransportType.NONE
            )
        }
    }

    /**
     * Parse network capabilities into status
     */
    private fun parseNetworkCapabilities(
        network: Network,
        capabilities: NetworkCapabilities
    ): NetworkStatus {
        val transportType = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                NetworkStatus.TransportType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                NetworkStatus.TransportType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                NetworkStatus.TransportType.ETHERNET
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ->
                NetworkStatus.TransportType.BLUETOOTH
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
                NetworkStatus.TransportType.VPN
            else -> NetworkStatus.TransportType.NONE
        }

        val signalStrength = capabilities.signalStrength
        
        return NetworkStatus(
            isAvailable = true,
            isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            transportType = transportType,
            signalStrength = if (signalStrength in 0..100) signalStrength else null
        )
    }

    /**
     * Manual connectivity check (for diagnostics)
     */
    fun checkConnectivity(): NetworkStatus {
        return getCurrentNetworkStatus()
    }

    /**
     * Get time since last network event
     */
    fun getTimeSinceLastAvailable(): Long {
        return if (lastAvailableTime > 0) {
            System.currentTimeMillis() - lastAvailableTime
        } else {
            -1
        }
    }

    fun getTimeSinceLastLost(): Long {
        return if (lastLostTime > 0) {
            System.currentTimeMillis() - lastLostTime
        } else {
            -1
        }
    }

    /**
     * Cleanup
     */
    fun shutdown() {
        scope.cancel()
    }
}
