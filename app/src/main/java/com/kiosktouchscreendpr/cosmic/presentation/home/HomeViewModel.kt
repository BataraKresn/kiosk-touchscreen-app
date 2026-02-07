package com.kiosktouchscreendpr.cosmic.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.utils.ConnectivityObserver
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import com.kiosktouchscreendpr.cosmic.data.datasource.refreshmechanism.RefreshRes
import com.kiosktouchscreendpr.cosmic.data.api.DeviceApi
import com.kiosktouchscreendpr.cosmic.domain.usecase.RemoteRefreshUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preference: Preference,
    private val refreshUseCase: RemoteRefreshUseCase,
    private val connectivityObserver: ConnectivityObserver,
    private val deviceApi: DeviceApi
) : ViewModel() {

    private val webviewUrl = BuildConfig.WEBVIEW_BASEURL
    private val wsUrl = BuildConfig.WS_URL

    private val token
        get() = preference.get("token", null)

    private val timeout
        get() = preference.get("timeout", null)?.toIntOrNull() ?: 1

    init {
        checkExistingDeviceAndAutoStart()
    }

    private val _state = MutableStateFlow(HomeState())
    val state = _state
        .onStart { loadBaseUrl() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value
        )

    private var inactivityTimerJob: Job? = null
    private val _resetEvent = MutableSharedFlow<Unit>()
    val resetEvent = _resetEvent.asSharedFlow()

    private var isOnBaseUrl = true
    private var lastRemoteRefreshAt = 0L


    private fun loadBaseUrl() = viewModelScope.launch {
        _state.update { it.copy(isLoadingUrl = true) }

        _state.update {
            it.copy(
                baseUrl = "$webviewUrl/display/$token",
                timeout = timeout,
                initialUrl = true,
                isLoadingUrl = false
            )
        }
    }

    fun triggerRefresh(trigger: () -> Unit) = viewModelScope.launch {
        refreshUseCase.observeMessages().collect { message ->
            when (message) {
                is RefreshRes.Triggered -> {
                    if (message.token == token) {
                        val now = System.currentTimeMillis()
                        if (now - lastRemoteRefreshAt > 30_000L) {
                            lastRemoteRefreshAt = now
                            trigger()
                        }
                    }
                }

                is RefreshRes.Error -> Unit
            }
        }
    }

    fun observeNetworks(reload: () -> Unit) = viewModelScope.launch {
        connectivityObserver.isConnected.collect { connected ->
            if (connected) {
                println("connected to the internet")
                reload()
                refreshUseCase.connect("$wsUrl/ws")
            } else {
                println("not connected to the internet")
                refreshUseCase.disconnect()
            }
        }
    }
    
    /**
     * Periodic check untuk schedule changes setiap 1 menit
     * DISABLED - Tidak perlu auto-refresh di display screen
     * User bisa manual refresh dengan 5 taps atau WebSocket trigger
     */
    fun startPeriodicRefresh(reload: () -> Unit) = viewModelScope.launch {
        // Disabled - tidak ada auto-refresh
        // while (true) {
        //     delay(60_000L) // 1 menit
        //     println("⏰ Periodic refresh check...")
        //     reload()
        // }
    }

    fun onUrlChanged(currentUrl: String) {
        val onBasePage = currentUrl == state.value.baseUrl
        isOnBaseUrl = onBasePage

        if (!onBasePage) {
            startInactivityTimer()
        } else {
            cancelInactivityTimer()
        }
    }

    fun onUserInteraction() {
        if (!isOnBaseUrl) {
            startInactivityTimer()
        }
    }


    private fun startInactivityTimer() {
        cancelInactivityTimer()
        inactivityTimerJob = viewModelScope.launch {
            delay(_state.value.timeout * 60 * 1000L)
            _resetEvent.emit(Unit)
        }
    }


    private fun cancelInactivityTimer() {
        inactivityTimerJob?.cancel()
        inactivityTimerJob = null
    }

    /**
     * Check if device exists in CMS and auto-start remote control if device found
     */
    private fun checkExistingDeviceAndAutoStart() = viewModelScope.launch {
        try {
            val deviceId = preference.get("device_id", null)
            if (deviceId.isNullOrEmpty()) {
                println("HomeViewModel: No device_id found, skipping existing device check")
                return@launch
            }

            println("HomeViewModel: Checking existing device: $deviceId")
            
            // Try to check existing device via API
            val existingDevice = try {
                deviceApi.checkExistingDevice(
                    baseUrl = webviewUrl,
                    deviceId = deviceId
                )
            } catch (e: Exception) {
                println("HomeViewModel: API checkExistingDevice failed: ${e.message}")
                null
            }
            
            if (existingDevice != null && 
                existingDevice.success &&
                existingDevice.data.remoteId > 0 && 
                existingDevice.data.token.isNotEmpty()) {
                
                println("HomeViewModel: Device exists via API, storing credentials and auto-starting")
                preference.set("remote_id", existingDevice.data.remoteId.toString())
                preference.set("remote_token", existingDevice.data.token)
                startRemoteControlService()
                
            } else {
                // Fallback: Check if we already have stored credentials
                val storedRemoteId = preference.get("remote_id", null)
                val storedRemoteToken = preference.get("remote_token", null)
                
                if (!storedRemoteId.isNullOrEmpty() && !storedRemoteToken.isNullOrEmpty()) {
                    println("HomeViewModel: Using stored credentials for auto-start")
                    println("HomeViewModel: Remote ID: $storedRemoteId, Token: ${storedRemoteToken.take(20)}...")
                    startRemoteControlService()
                } else {
                    println("HomeViewModel: No stored credentials found, skipping auto-start")
                }
            }
        } catch (e: Exception) {
            println("HomeViewModel: Error in auto-start check: ${e.message}")
        }
    }

    private fun startRemoteControlService() {
        // TODO: Add logic to start RemoteControlService with auto_start=true
        // This will be implemented when we have service injection or context access
        println("HomeViewModel: Auto-start should trigger RemoteControlService")
    }

    override fun onCleared() {
        super.onCleared()
        cancelInactivityTimer()
    }
}