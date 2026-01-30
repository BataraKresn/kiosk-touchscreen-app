package com.kiosktouchscreendpr.cosmic.presentation.settings

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.core.scheduler.AlarmItem
import com.kiosktouchscreendpr.cosmic.core.scheduler.AlarmType
import com.kiosktouchscreendpr.cosmic.core.scheduler.PowerOffSchedule
import com.kiosktouchscreendpr.cosmic.core.scheduler.PowerOnSchedule
import com.kiosktouchscreendpr.cosmic.data.api.DeviceApi
import com.kiosktouchscreendpr.cosmic.data.dto.RegisterDeviceRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: SharedPreferences,
    private val powerOffSchedule: PowerOffSchedule,
    private val powerOnSchedule: PowerOnSchedule,
    private val deviceApi: DeviceApi,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    
    // Mapping NAME → TOKEN dari CMS
    private var displayNameToToken = mutableMapOf<String, String>()

    val state: StateFlow<SettingsState> = _state
        .onStart { loadSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = _state.value

        )


    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.OnTokenChanged -> {
                _state.value = _state.value.copy(token = event.token)
            }

            is SettingsEvent.OnTimeoutChanged -> {
                _state.value = _state.value.copy(timeout = event.timeout)
            }

            is SettingsEvent.OnPowerOffTimeChanged -> {
                _state.value = _state.value.copy(powerOffTime = event.powerOffTime)
            }

            is SettingsEvent.OnPowerOnTimeChanged -> {
                _state.value = _state.value.copy(powerOnTime = event.powerOnTime)
            }

            SettingsEvent.OnRefreshTokens -> {
                fetchTokensFromCms()
            }

            is SettingsEvent.OnSelectToken -> {
                // event.token adalah NAME, convert ke TOKEN
                val actualToken = displayNameToToken[event.token] ?: event.token
                _state.value = _state.value.copy(token = actualToken)
            }

            SettingsEvent.OnSubmit -> {
                if (_state.value.token.isEmpty()) {
                    _state.value = _state.value.copy(errorMessage = "Token tidak boleh kosong")
                    return
                }
                if (_state.value.timeout.isEmpty()) {
                    _state.value = _state.value.copy(errorMessage = "Timeout tidak boleh kosong")
                    return
                }
                if (_state.value.powerOffTime == null) {
                    _state.value =
                        _state.value.copy(errorMessage = "Waktu power off tidak boleh kosong")
                    return
                }
                if (_state.value.powerOnTime == null) {
                    _state.value =
                        _state.value.copy(errorMessage = "Waktu power on tidak boleh kosong")
                    return
                }

                onSubmit()
            }
        }
    }

    private fun onSubmit() = viewModelScope.launch {
        // Save to SharedPreferences
        preferences.edit().apply {
            putString(AppConstant.TOKEN, _state.value.token)
            putString(AppConstant.TIMEOUT, _state.value.timeout)
            putString(AppConstant.POWER_OFF, formatTime(_state.value.powerOffTime))
            putString(AppConstant.POWER_ON, formatTime(_state.value.powerOnTime))
            apply()
        }

        // Register display to backend (optional, non-blocking)
        registerDisplayToken(_state.value.token)

        // Schedule alarms
        _state.value.powerOffTime?.let { powerOff ->
            _state.value.powerOnTime?.let { powerOn ->
                scheduleAlarmInternal(powerOff, powerOn)
            }
        }
        _state.update { it.copy(isSuccess = true, errorMessage = null) }
    }
    
    /**
     * Register display token ke backend CMS
     * Non-blocking, tidak akan error jika backend tidak ada atau gagal
     */
    private fun registerDisplayToken(token: String) = viewModelScope.launch {
        try {
            val baseUrl = BuildConfig.WEBVIEW_BASEURL
            val request = RegisterDeviceRequest(
                token = token,
                name = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                deviceInfo = com.kiosktouchscreendpr.cosmic.data.dto.DeviceMetadata(
                    manufacturer = android.os.Build.MANUFACTURER,
                    model = android.os.Build.MODEL,
                    androidVersion = android.os.Build.VERSION.RELEASE,
                    appVersion = BuildConfig.VERSION_NAME
                )
            )
            
            val response = deviceApi.registerDisplayToken(baseUrl, request)
            
            if (response != null) {
                Log.d(TAG, "✅ Display token registered: $token")
            } else {
                Log.w(TAG, "⚠️ Failed to register display token, but continuing anyway")
            }
        } catch (e: Exception) {
            // Tidak perlu error karena ini optional
            Log.w(TAG, "⚠️ Error registering display token: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private fun loadSettings() = viewModelScope.launch {
        val powerOff = preferences.getString(AppConstant.POWER_OFF, null)
        val powerOn = preferences.getString(AppConstant.POWER_ON, null)
        
        // Load token dari SharedPreferences
        val savedToken = preferences.getString(AppConstant.TOKEN, "") ?: ""

        _state.value = SettingsState(
            token = savedToken,
            timeout = preferences.getString(AppConstant.TIMEOUT, "") ?: "",
            powerOffTime = parseTime(powerOff ?: ""),
            powerOnTime = parseTime(powerOn ?: "")
        )

        // Jangan auto-fetch CMS di sini, hanya fetch ketika user click button
    }

    private fun fetchTokensFromCms() = viewModelScope.launch {
        _state.update { it.copy(isLoadingTokens = true, tokenLoadError = null) }
        try {
            val baseUrl = BuildConfig.WEBVIEW_BASEURL
            val response = deviceApi.getDisplays(baseUrl, perPage = 50)
            
            // Build mapping NAME → TOKEN
            displayNameToToken.clear()
            response?.data?.forEach { display ->
                if (display.name != null && display.token != null) {
                    displayNameToToken[display.name] = display.token
                }
            }
            
            val tokens = response?.data
                ?.mapNotNull { it.name }  // Show NAME di dropdown
                ?.distinct()
                ?.sorted()
                ?: emptyList()

            _state.update {
                it.copy(
                    availableTokens = tokens,
                    isLoadingTokens = false,
                    tokenLoadError = if (tokens.isEmpty()) "Display CMS kosong" else null
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoadingTokens = false,
                    tokenLoadError = e.message ?: "Gagal mengambil token dari CMS"
                )
            }
        }
    }

    private fun scheduleAlarmInternal(
        powerOffTime: Pair<Int, Int>,
        powerOnTime: Pair<Int, Int>,
    ) {
        val powerOffItem = AlarmItem(
            hour = powerOffTime.first, minute = powerOffTime.second, type = AlarmType.LOCK
        )

        val powerOnItem = AlarmItem(
            hour = powerOnTime.first, minute = powerOnTime.second, type = AlarmType.WAKE
        )

        powerOffSchedule.cancel(powerOffItem)
        powerOnSchedule.cancel(powerOnItem)

        powerOffSchedule.schedule(powerOffItem)
        powerOnSchedule.schedule(powerOnItem)
    }

    private fun parseTime(timeString: String?): Pair<Int, Int>? {
        return timeString?.split(":")?.takeIf { it.size == 2 }?.let {
            runCatching { Pair(it[0].toInt(), it[1].toInt()) }.getOrNull()
        }
    }

    private fun formatTime(time: Pair<Int, Int>?): String {
        return time?.let { String.format(Locale.getDefault(), "%02d:%02d", it.first, it.second) }
            ?: ""
    }

}