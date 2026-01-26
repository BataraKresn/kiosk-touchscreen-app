package com.kiosktouchscreendpr.cosmic.presentation.settings

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.core.scheduler.AlarmItem
import com.kiosktouchscreendpr.cosmic.core.scheduler.AlarmType
import com.kiosktouchscreendpr.cosmic.core.scheduler.PowerOffSchedule
import com.kiosktouchscreendpr.cosmic.core.scheduler.PowerOnSchedule
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
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())

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
        preferences.edit().apply {
            putString(AppConstant.TOKEN, _state.value.token)
            putString(AppConstant.TIMEOUT, _state.value.timeout)
            putString(AppConstant.POWER_OFF, formatTime(_state.value.powerOffTime))
            putString(AppConstant.POWER_ON, formatTime(_state.value.powerOnTime))
            apply()
        }

        _state.value.powerOffTime?.let { powerOff ->
            _state.value.powerOnTime?.let { powerOn ->
                scheduleAlarmInternal(powerOff, powerOn)
            }
        }
        _state.update { it.copy(isSuccess = true, errorMessage = null) }
    }

    private fun loadSettings() = viewModelScope.launch {
        val powerOff = preferences.getString(AppConstant.POWER_OFF, null)
        val powerOn = preferences.getString(AppConstant.POWER_ON, null)

        _state.value = SettingsState(
            token = preferences.getString(AppConstant.TOKEN, "") ?: "",
            timeout = preferences.getString(AppConstant.TIMEOUT, "") ?: "",
            powerOffTime = parseTime(powerOff ?: ""),
            powerOnTime = parseTime(powerOn ?: "")
        )
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