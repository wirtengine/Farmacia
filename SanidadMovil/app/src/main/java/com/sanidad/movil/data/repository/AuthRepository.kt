package com.sanidad.movil.data.repository

import com.sanidad.movil.data.local.TokenDataStore
import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.LoginRequest
import com.sanidad.movil.data.remote.dto.LoginResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val api: ApiService = NetworkModule.apiService,
    private val tokenDataStore: TokenDataStore
) {
    suspend fun login(username: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful) {
                val loginResponse = response.body()!!
                tokenDataStore.saveToken(loginResponse.token)
                NetworkModule.setToken(loginResponse.token)
                Result.success(loginResponse)
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenDataStore.clearToken()
        NetworkModule.setToken(null)
    }

    fun isLoggedIn(): Flow<Boolean> {
        return tokenDataStore.tokenFlow.map { it != null }
    }

    suspend fun getSavedToken(): String? {
        return tokenDataStore.tokenFlow.map { it }.toString() // mejor usar first()
    }
}