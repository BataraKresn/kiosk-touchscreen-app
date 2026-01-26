package com.kiosktouchscreendpr.cosmic.presentation.settings

sealed interface SettingsEvent {
    data class OnTokenChanged(val token: String) : SettingsEvent
    data class OnTimeoutChanged(val timeout: String) : SettingsEvent
    data class OnPowerOffTimeChanged(val powerOffTime: Pair<Int, Int>) : SettingsEvent
    data class OnPowerOnTimeChanged(val powerOnTime: Pair<Int, Int>) : SettingsEvent
    data object OnSubmit : SettingsEvent
}