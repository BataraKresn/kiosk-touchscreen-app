package com.kiosktouchscreendpr.cosmic.presentation.settings

data class SettingsState(
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val token: String = "",
    val timeout: String = "",
    val powerOffTime: Pair<Int, Int>? = null,
    val powerOnTime: Pair<Int, Int>? = null,
    val availableTokens: List<String> = emptyList(),
    val isLoadingTokens: Boolean = false,
    val tokenLoadError: String? = null,
)