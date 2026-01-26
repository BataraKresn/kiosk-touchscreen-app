package com.kiosktouchscreendpr.cosmic.presentation.auth

data class AuthState(
    val isAuthenticated: Boolean = false,
    val password: String = "",
    val errorMessage: String? = null
)