package com.sanidad.movil.data.repository

import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.LoginRequest
import com.sanidad.movil.data.remote.dto.LoginResponse
import com.sanidad.movil.data.remote.safeApiCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.sanidad.movil.data.remote.ApiResult

class AuthRepository(
    private val api: ApiService = NetworkModule.apiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun login(username: String, password: String): ApiResult<LoginResponse> {
        val result = safeApiCall {
            api.login(LoginRequest(username, password))
        }
        if (result is ApiResult.Success) {
            tokenDataStore.saveToken(result.data.token)
            NetworkModule.setToken(result.data.token)
        }
        return result
    }

    suspend fun logout() {
        tokenDataStore.clearToken()
        NetworkModule.setToken(null)
    }

    fun isLoggedIn(): Flow<Boolean> {
        return tokenDataStore.tokenFlow.map { it != null }
    }
}