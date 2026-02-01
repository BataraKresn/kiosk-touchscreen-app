package com.kiosktouchscreendpr.cosmic.core.connection

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.kiosktouchscreendpr.cosmic.core.utils.DeviceHealthMonitor
import com.kiosktouchscreendpr.cosmic.data.api.DeviceRegistrationService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * SINGLE CONNECTION STATE OWNER
 * 
 * This manager is the SOLE authority for connection lifecycle.
 * All connection decisions flow through this manager.
 * 
 * Key Principles:
 * 1. CMS heartbeat response is AUTHORITATIVE
 * 2. Network events are signals, not commands
 * 3. All reconnects must be serialized and server-approved
 * 4. Lifecycle and Doze modes modulate behavior, not control it
 * 
 * Fixes Applied:
 * - FIX #1: Single connection state machine
 * - FIX #2: HTTP heartbeat controls all reconnect logic
 * - FIX #3: Server-initiated reconnect compliance (should_reconnect, reconnect_delay)
 * - FIX #4: Network observer with debounce and stability window
 * - FIX #5: ProcessLifecycleOwner integration
 * - FIX #6: Doze mode detection and adaptation
 * - FIX #7: Clean timeout handling
 * - FIX #8: Reconnect strategy with jitter, ceiling, and circuit breaker
 * - FIX #9: Lifecycle-bound coroutines, no duplicate jobs
 * 
 * @author Cosmic Development Team (Anti-Flapping Edition)
 */
@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRegistrationService: DeviceRegistrationService,
    private val deviceHealthMonitor: DeviceHealthMonitor
) {

    companion object {
        private const val TAG = "ConnectionManager"
        
        // Heartbeat intervals
        private const val HEARTBEAT_INTERVAL_FOREGROUND_MS = 30_000L // 30s
        private const val HEARTBEAT_INTERVAL_BACKGROUND_MS = 90_000L // 90s
        private const val HEARTBEAT_INTERVAL_DOZE_MS = 300_000L // 5min
        
        // Timeout thresholds
        private const val HEARTBEAT_TIMEOUT_FOREGROUND_MS = 60_000L // 1min
        private const val HEARTBEAT_TIMEOUT_BACKGROUND_MS = 180_000L // 3min
        private const val HEARTBEAT_TIMEOUT_DOZE_MS = 600_000L // 10min
        
        // Network debounce
        private const val NETWORK_STABILITY_WINDOW_MS = 3000L // 3s
        
        // Reconnect strategy
        private const val INITIAL_RECONNECT_DELAY_MS = 2000L
        private const val MAX_RECONNECT_DELAY_MS = 120_000L // 2min
        private const val RECONNECT_BACKOFF_BASE = 2.0
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val CIRCUIT_BREAKER_THRESHOLD = 5
        private const val CIRCUIT_BREAKER_RESET_MS = 300_000L // 5min
    }

    /**
     * Connection State Machine
     */
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val since: Long) : ConnectionState()
        data class Reconnecting(val attempt: Int, val nextAttemptAt: Long) : ConnectionState()
        data class ServerBlocked(val until: Long?) : ConnectionState() // CMS said NO
        data class CircuitOpen(val since: Long) : ConnectionState() // Too many failures
        data class Error(val message: String, val retryable: Boolean) : ConnectionState()
    }

    /**
     * Process State (from ProcessLifecycleOwner)
     */
    private enum class ProcessState {
        FOREGROUND, BACKGROUND
    }

    /**
     * Power State (Android Doze)
     */
    private enum class PowerState {
        NORMAL, LIGHT_DOZE, DEEP_DOZE
    }

    // State flows
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _processState = MutableStateFlow(ProcessState.FOREGROUND)
    private val _powerState = MutableStateFlow(PowerState.NORMAL)
    private val _networkStable = MutableStateFlow(false)

    // Connection metadata
    private var deviceToken: String? = null
    private var lastSuccessfulHeartbeat: Long = 0
    private var reconnectAttempt = 0
    private var consecutiveFailures = 0
    private var circuitBreakerOpenedAt: Long = 0

    // Coroutine management
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var networkDebounceJob: Job? = null

    // Power manager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    init {
        observeProcessLifecycle()
        observePowerState()
    }

    /**
     * FIX #5: Lifecycle Integration
     * Monitor app foreground/background state
     */
    private fun observeProcessLifecycle() {
        managerScope.launch {
            while (isActive) {
                val lifecycle = ProcessLifecycleOwner.get().lifecycle
                val isForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                
                _processState.value = if (isForeground) {
                    ProcessState.FOREGROUND
                } else {
                    ProcessState.BACKGROUND
                }
                
                Log.d(TAG, "Process state: ${_processState.value}")
                
                // Adjust heartbeat when process state changes
                if (heartbeatJob?.isActive == true) {
                    restartHeartbeatWithCurrentSettings()
                }
                
                delay(5000) // Check every 5s
            }
        }
    }

    /**
     * FIX #6: Doze Mode Detection
     * Detect Light/Deep Doze and adjust behavior
     */
    private fun observePowerState() {
        managerScope.launch {
            while (isActive) {
                _powerState.value = when {
                    powerManager.isDeviceIdleMode -> PowerState.DEEP_DOZE
                    powerManager.isPowerSaveMode -> PowerState.LIGHT_DOZE
                    else -> PowerState.NORMAL
                }
                
                if (_powerState.value != PowerState.NORMAL) {
                    Log.d(TAG, "Power state: ${_powerState.value}")
                }
                
                delay(10000) // Check every 10s
            }
        }
    }

    /**
     * FIX #4: Network Debouncing
     * Network events do NOT directly trigger reconnect.
     * They set a stability flag after a window of consistent connectivity.
     */
    fun onNetworkAvailable() {
        networkDebounceJob?.cancel()
        networkDebounceJob = managerScope.launch {
            delay(NETWORK_STABILITY_WINDOW_MS)
            _networkStable.value = true
            Log.d(TAG, "Network stable after debounce window")
            
            // Network stability is a SIGNAL, not a command
            // If we have a token but not connected, try to connect now
            // Only start if heartbeat not already running
            if (deviceToken != null && _connectionState.value !is ConnectionState.Connected && heartbeatJob?.isActive != true) {
                Log.i(TAG, "Network stable and have token, attempting connection")
                startHeartbeat()
            } else if (_connectionState.value is ConnectionState.Disconnected ||
                _connectionState.value is ConnectionState.Error) {
                considerReconnect("Network became stable")
            }
        }
    }

    fun onNetworkLost() {
        networkDebounceJob?.cancel()
        _networkStable.value = false
        Log.d(TAG, "Network lost")
        
        // Clean disconnect, no immediate reconnect
        disconnect(reason = "Network lost")
    }

    /**
     * Primary connect entry point
     */
    fun connect(token: String) {
        // Save token FIRST before any checks
        deviceToken = token
        reconnectAttempt = 0
        
        if (_connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Already connected")
            return
        }

        if (_connectionState.value is ConnectionState.ServerBlocked) {
            val blocked = _connectionState.value as ConnectionState.ServerBlocked
            val until = blocked.until
            if (until != null && System.currentTimeMillis() < until) {
                Log.w(TAG, "Connection blocked by server until ${until - System.currentTimeMillis()}ms")
                return
            }
        }

        if (!_networkStable.value) {
            Log.w(TAG, "Network not stable, deferring connection (token saved)")
            return
        }
        
        _connectionState.value = ConnectionState.Connecting
        startHeartbeat()
    }

    /**
     * FIX #1 & #2: Unified Heartbeat
     * HTTP heartbeat to CMS is the ONLY source of truth for connection state.
     * WebSocket heartbeat (if any) is for transport health only.
     */
    private fun startHeartbeat() {
        // Guard: Don't start if already running
        if (heartbeatJob?.isActive == true) {
            Log.d(TAG, "Heartbeat already running, skipping start")
            return
        }
        
        stopHeartbeat()
        
        Log.i(TAG, "Starting heartbeat with token: ${deviceToken?.take(8)}...")
        
        heartbeatJob = managerScope.launch {
            while (isActive && deviceToken != null) {
                val interval = getCurrentHeartbeatInterval()
                val timeout = getCurrentHeartbeatTimeout()
                
                try {
                    val result = sendHeartbeatToCMS()
                    
                    when {
                        result.isSuccess -> {
                            handleSuccessfulHeartbeat(result.getOrNull()!!)
                        }
                        else -> {
                            handleFailedHeartbeat(result.exceptionOrNull()!!, timeout)
                        }
                    }
                    
                } catch (e: CancellationException) {
                    Log.d(TAG, "Heartbeat cancelled")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat error", e)
                    handleFailedHeartbeat(e, timeout)
                }
                
                // Delay after sending, not before, so first heartbeat is immediate
                delay(interval)
            }
        }
    }

    /**
     * FIX #7: Timeout Correction
     * Timeout immediately terminates session and transitions state cleanly
     * 
     * FIX #10: Device Health Metrics Integration
     * Collect and send real-time device metrics with every heartbeat
     */
    private suspend fun sendHeartbeatToCMS(): Result<DeviceRegistrationService.HeartbeatData> {
        val token = deviceToken ?: return Result.failure(Exception("No device token"))
        
        val timeout = getCurrentHeartbeatTimeout()
        
        return withTimeout(timeout) {
            try {
                // Collect all device health metrics
                val metrics = deviceHealthMonitor.getAllMetrics()
                
                deviceRegistrationService.sendHeartbeat(
                    token = token,
                    batteryLevel = metrics.batteryLevel,
                    wifiStrength = metrics.wifiStrength,
                    screenOn = metrics.screenOn,
                    storageAvailableMB = metrics.storageAvailableMB,
                    storageTotalMB = metrics.storageTotalMB,
                    ramUsageMB = metrics.ramUsageMB,
                    ramTotalMB = metrics.ramTotalMB,
                    cpuTemp = metrics.cpuTemp,
                    networkType = metrics.networkType,
                    currentUrl = null // TODO: Get from WebView if needed
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * FIX #3: Server-Initiated Reconnect Compliance
     * CMS response dictates reconnect behavior via should_reconnect
     */
    private suspend fun handleSuccessfulHeartbeat(data: DeviceRegistrationService.HeartbeatData) {
        lastSuccessfulHeartbeat = System.currentTimeMillis()
        consecutiveFailures = 0
        
        // Reset circuit breaker on success
        if (_connectionState.value is ConnectionState.CircuitOpen) {
            Log.i(TAG, "Circuit breaker reset after successful heartbeat")
        }
        
        // Transition to connected if we weren't already
        if (_connectionState.value !is ConnectionState.Connected) {
            _connectionState.value = ConnectionState.Connected(System.currentTimeMillis())
            Log.i(TAG, "Connection established")
        }
        
        // CRITICAL: Obey server reconnect instructions
        if (!data.should_reconnect) {
            // SERVER SAYS NO - immediately block reconnects
            Log.w(TAG, "⛔ Server instructed: DO NOT RECONNECT")
            _connectionState.value = ConnectionState.ServerBlocked(until = null) // Indefinite block
            stopHeartbeat()
            return
        }
    }

    /**
     * Handle heartbeat failure
     */
    private suspend fun handleFailedHeartbeat(error: Throwable, timeout: Long) {
        consecutiveFailures++
        
        val timeSinceLastSuccess = System.currentTimeMillis() - lastSuccessfulHeartbeat
        
        // FIX #7: Timeout detection
        if (timeSinceLastSuccess > timeout) {
            Log.e(TAG, "❌ Heartbeat timeout after ${timeSinceLastSuccess}ms (threshold: ${timeout}ms)")
            
            // Clean termination
            stopHeartbeat()
            _connectionState.value = ConnectionState.Error(
                message = "Heartbeat timeout",
                retryable = true
            )
            
            // FIX #8: Circuit breaker
            if (consecutiveFailures >= CIRCUIT_BREAKER_THRESHOLD) {
                openCircuitBreaker()
            } else {
                considerReconnect("Heartbeat timeout")
            }
        } else {
            Log.w(TAG, "Heartbeat failed (attempt $consecutiveFailures): ${error.message}")
        }
    }

    /**
     * FIX #8: Circuit Breaker
     * Too many consecutive failures → stop trying for a while
     */
    private fun openCircuitBreaker() {
        circuitBreakerOpenedAt = System.currentTimeMillis()
        _connectionState.value = ConnectionState.CircuitOpen(circuitBreakerOpenedAt)
        Log.e(TAG, "🚨 Circuit breaker opened after $consecutiveFailures failures")
        
        // Schedule circuit breaker reset
        managerScope.launch {
            delay(CIRCUIT_BREAKER_RESET_MS)
            if (_connectionState.value is ConnectionState.CircuitOpen) {
                Log.i(TAG, "Circuit breaker auto-reset after cooldown")
                consecutiveFailures = 0
                considerReconnect("Circuit breaker reset")
            }
        }
    }

    /**
     * FIX #8: Reconnect Strategy
     * Serialized, idempotent reconnect with exponential backoff + jitter
     */
    private fun considerReconnect(reason: String) {
        // Guard: Don't reconnect if blocked
        when (val state = _connectionState.value) {
            is ConnectionState.ServerBlocked -> {
                val until = state.until
                if (until != null && System.currentTimeMillis() < until) {
                    Log.w(TAG, "Reconnect blocked by server")
                    return
                }
            }
            is ConnectionState.CircuitOpen -> {
                Log.w(TAG, "Reconnect blocked by circuit breaker")
                return
            }
            is ConnectionState.Connecting, is ConnectionState.Reconnecting -> {
                Log.d(TAG, "Already attempting to connect")
                return
            }
            is ConnectionState.Connected -> {
                Log.d(TAG, "Already connected")
                return
            }
            else -> { /* proceed */ }
        }

        // Guard: Check network stability
        if (!_networkStable.value) {
            Log.w(TAG, "Cannot reconnect: network unstable")
            return
        }

        // Guard: Max attempts
        if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnect attempts reached ($MAX_RECONNECT_ATTEMPTS)")
            openCircuitBreaker()
            return
        }

        // Cancel any pending reconnect
        reconnectJob?.cancel()
        
        reconnectJob = managerScope.launch {
            reconnectAttempt++
            
            // Exponential backoff with jitter
            val baseDelay = INITIAL_RECONNECT_DELAY_MS * RECONNECT_BACKOFF_BASE.pow(reconnectAttempt - 1).toLong()
            val cappedDelay = min(baseDelay, MAX_RECONNECT_DELAY_MS)
            val jitter = Random.nextLong(0, cappedDelay / 4) // ±25% jitter
            val delay = cappedDelay + jitter
            
            val nextAttemptAt = System.currentTimeMillis() + delay
            _connectionState.value = ConnectionState.Reconnecting(reconnectAttempt, nextAttemptAt)
            
            Log.i(TAG, "🔄 Reconnect attempt $reconnectAttempt in ${delay}ms (reason: $reason)")
            
            delay(delay)
            
            // Re-check guards after delay
            if (!_networkStable.value) {
                Log.w(TAG, "Network became unstable during backoff")
                return@launch
            }
            
            val token = deviceToken
            if (token != null) {
                _connectionState.value = ConnectionState.Connecting
                startHeartbeat()
            } else {
                Log.e(TAG, "Cannot reconnect: no device token")
            }
        }
    }

    /**
     * Disconnect cleanly
     */
    fun disconnect(reason: String) {
        Log.i(TAG, "Disconnecting: $reason")
        stopHeartbeat()
        reconnectJob?.cancel()
        networkDebounceJob?.cancel()
        reconnectAttempt = 0
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Stop heartbeat job
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Restart heartbeat with current settings (e.g., after lifecycle change)
     */
    private fun restartHeartbeatWithCurrentSettings() {
        if (_connectionState.value is ConnectionState.Connected && deviceToken != null) {
            startHeartbeat()
        }
    }

    /**
     * Get current heartbeat interval based on process and power state
     */
    private fun getCurrentHeartbeatInterval(): Long {
        return when {
            _powerState.value == PowerState.DEEP_DOZE -> HEARTBEAT_INTERVAL_DOZE_MS
            _processState.value == ProcessState.BACKGROUND -> HEARTBEAT_INTERVAL_BACKGROUND_MS
            else -> HEARTBEAT_INTERVAL_FOREGROUND_MS
        }
    }

    /**
     * Get current heartbeat timeout based on process and power state
     */
    private fun getCurrentHeartbeatTimeout(): Long {
        return when {
            _powerState.value == PowerState.DEEP_DOZE -> HEARTBEAT_TIMEOUT_DOZE_MS
            _processState.value == ProcessState.BACKGROUND -> HEARTBEAT_TIMEOUT_BACKGROUND_MS
            else -> HEARTBEAT_TIMEOUT_FOREGROUND_MS
        }
    }

    /**
     * Clean shutdown
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down ConnectionManager")
        disconnect("Shutdown")
        managerScope.cancel()
    }
}
