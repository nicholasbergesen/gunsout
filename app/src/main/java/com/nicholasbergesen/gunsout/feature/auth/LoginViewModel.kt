package com.nicholasbergesen.gunsout.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicholasbergesen.gunsout.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun signIn(activityContext: Context) {
        if (_state.value is LoginUiState.Loading) return
        _state.value = LoginUiState.Loading
        viewModelScope.launch {
            when (val result = authRepository.signIn(activityContext)) {
                is AuthRepository.SignInResult.Success -> {
                    // AuthGate observes signedInUser and will transition to SetupScreen.
                    _state.value = LoginUiState.Idle
                }
                AuthRepository.SignInResult.Cancelled -> {
                    _state.value = LoginUiState.Idle
                }
                is AuthRepository.SignInResult.NoGoogleAccount -> {
                    _state.value = LoginUiState.Error(result.message)
                }
                is AuthRepository.SignInResult.ConfigurationError -> {
                    _state.value = LoginUiState.Error(result.message)
                }
                is AuthRepository.SignInResult.UnknownError -> {
                    _state.value = LoginUiState.Error(result.message)
                }
            }
        }
    }
}
