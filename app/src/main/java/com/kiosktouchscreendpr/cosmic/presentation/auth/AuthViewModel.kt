package com.kiosktouchscreendpr.cosmic.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kiosktouchscreendpr.cosmic.BuildConfig
import com.kiosktouchscreendpr.cosmic.core.constant.AppConstant
import com.kiosktouchscreendpr.cosmic.core.utils.Preference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val preferences: Preference
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = AuthState()
    )

    private val predefinedPassword = BuildConfig.APP_PASSWORD

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.OnPasswordChange -> {
                _state.update {
                    it.copy(password = event.password)
                }
            }

            AuthEvent.OnSubmit -> appAuth(_state.value.password)
        }
    }

    private fun appAuth(password: String) = viewModelScope.launch {
        val defaultPassword = predefinedPassword
        if (password == defaultPassword) {
            preferences.set(AppConstant.AUTH, password)
            _state.update {
                it.copy(isAuthenticated = true, errorMessage = null)
            }
        } else {
            _state.update {
                it.copy(isAuthenticated = false, errorMessage = "password salah")
            }
        }
    }
}