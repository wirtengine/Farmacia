package com.sanidad.movil.presentation.screens.login

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sanidad.movil.data.remote.ApiResult
import com.sanidad.movil.data.remote.dto.LoginResponse
import com.sanidad.movil.data.repository.AuthRepository
import com.sanidad.movil.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

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
                    val loginResponse = result.data
                    // Si los datos vinieron en la respuesta, los usamos
                    if (loginResponse.id != 0L && loginResponse.username.isNotEmpty()) {
                        UserSession.userId = loginResponse.id
                        UserSession.username = loginResponse.username
                        UserSession.rol = loginResponse.rol
                    } else {
                        // Fallback: decodificar el token JWT
                        try {
                            val token = loginResponse.token
                            val parts = token.split(".")
                            if (parts.size == 3) {
                                val payload = String(
                                    Base64.decode(parts[1], Base64.URL_SAFE),
                                    StandardCharsets.UTF_8
                                )
                                val json = JSONObject(payload)
                                UserSession.userId = json.optLong("userId", 0L)
                                UserSession.username = json.optString("sub", username)
                                UserSession.rol = json.optString("role", "VENDEDOR")
                            } else {
                                UserSession.userId = 0L
                                UserSession.username = username
                                UserSession.rol = "VENDEDOR"
                            }
                        } catch (e: Exception) {
                            UserSession.userId = 0L
                            UserSession.username = username
                            UserSession.rol = "VENDEDOR"
                        }
                    }
                    _uiState.value = LoginUiState(isSuccess = true)
                }
                is ApiResult.Error -> {
                    _uiState.value = LoginUiState(error = result.message)
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}