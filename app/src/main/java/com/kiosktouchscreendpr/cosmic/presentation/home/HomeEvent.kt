package com.kiosktouchscreendpr.cosmic.presentation.home

sealed interface HomeEvent {
    data class OnInitialUrl(val initialUrl: Boolean) : HomeEvent
    data class OnProgressChanged(val progress: Int) : HomeEvent
    data class OnUrlChanged(val currentUrl: String) : HomeEvent
    data object OnUserInteraction : HomeEvent
    data object ResetToHomePage : HomeEvent
}