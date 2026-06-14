package com.sanidad.movil.presentation.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.LoginResponse
import com.sanidad.movil.data.repository.AuthRepository
import com.sanidad.movil.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            when (val result = authRepository.login(username, password)) {
                is ApiResult.Success<LoginResponse> -> {
                    // Ahora result.data es de tipo LoginResponse
                    UserSession.userId = result.data.id
                    UserSession.username = result.data.username
                    UserSession.rol = result.data.rol
                    _uiState.value = LoginUiState(isSuccess = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = LoginUiState(error = result.message)
                }
                is ApiResult.Loading -> { /* no debería llegar aquí directamente */ }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}