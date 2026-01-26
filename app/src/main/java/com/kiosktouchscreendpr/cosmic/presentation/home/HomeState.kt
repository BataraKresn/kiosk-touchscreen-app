package com.kiosktouchscreendpr.cosmic.presentation.home

data class HomeState(
    val baseUrl: String = "",
    val initialUrl: Boolean = false,
    val isLoadingUrl: Boolean = false,
    val progress: Int = 0,
    val timeout: Int = 0,
)