package com.kiosktouchscreendpr.cosmic.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.app.AppState.Status
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.core.utils.ConnectivityObserver
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import com.kiosktouchscreendpr.cosmic.core.utils.formatLink
import com.kiosktouchscreendpr.cosmic.core.utils.getDeviceIP
import com.kiosktouchscreendpr.cosmic.data.datasource.heartbeat.Message
import com.kiosktouchscreendpr.cosmic.domain.usecase.WebSocketUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Code author  : Anugrah Surya Putra.
 * Project      : Cosmic
 */

@HiltViewModel
class AppViewModel @Inject constructor(
    private val preference: Preference,
    private val heartBeat: WebSocketUseCase,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val ipAddress: String? = getDeviceIP()
    private val formatLink: String = formatLink(ipAddress ?: "")

    private val websocketUrl = BuildConfig.WS_URL
    private val wsUrl = "$websocketUrl/ws_status_device?url=$formatLink"

    private val _state = MutableStateFlow(AppState())
    val state = _state
        .onStart {
            observeNetwork()
            observeWsMessages()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = _state.value
        )


    val isLoggedIn: String?
        get() = preference.get(AppConstant.AUTH, null)

    val token: String?
        get() = preference.get(AppConstant.TOKEN, null)

    private fun connectWs() = viewModelScope.launch {
        try {
            heartBeat.connect(wsUrl)
            _state.update { it.copy(status = Status.CONNECTED, error = null) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    status = Status.DISCONNECTED,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun disconnectWs() = viewModelScope.launch {
        heartBeat.disconnect()
        _state.update { it.copy(status = Status.DISCONNECTED, error = null) }
    }

    private fun observeNetwork() = viewModelScope.launch {
        connectivityObserver.isConnected.collect { connected ->
            if (connected) {
                println("🟢 Network available, trying to connect WebSocket")
                connectWs()
            } else {
                println("🔴 Network lost, disconnecting WebSocket")
                disconnectWs()
            }
        }
    }

    private fun observeWsMessages() = viewModelScope.launch {
        heartBeat.observeMessages().collect { message ->
            when (message) {

                is Message.Error -> {
                    _state.update { it.copy(status = Status.DISCONNECTED, error = message.message) }
                }

                is Message.Text -> {
                    // handle messages from server
                    /*println("Received text: ${message.content}")*/
                }

                else -> Unit
            }
        }
    }
}

data class AppState(
    val status: Status = Status.CONNECTING,
    val error: String? = null
) {
    enum class Status {
        DISCONNECTED, CONNECTING, CONNECTED
    }
}