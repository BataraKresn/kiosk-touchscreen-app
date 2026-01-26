package com.kiosktouchscreendpr.cosmic.presentation.auth

sealed interface AuthEvent {
    data class OnPasswordChange(val password: String) : AuthEvent
    data object OnSubmit : AuthEvent
}