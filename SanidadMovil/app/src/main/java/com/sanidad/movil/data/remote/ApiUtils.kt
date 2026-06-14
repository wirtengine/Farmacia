package com.sanidad.movil.data.remote

import retrofit2.Response
import java.io.IOException

sealed class ApiResult<out T> {
    object Loading : ApiResult<Nothing>()
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                @Suppress("UNCHECKED_CAST")
                ApiResult.Success(Unit as T)
            }
        } else {
            ApiResult.Error("Error ${response.code()}: ${response.message()}")
        }
    } catch (e: IOException) {
        ApiResult.Error("Error de conexión: ${e.localizedMessage}", e)
    } catch (e: Exception) {
        ApiResult.Error("Error inesperado: ${e.localizedMessage}", e)
    }
}

suspend fun safeApiCallUnit(apiCall: suspend () -> Response<Void>): ApiResult<Unit> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            ApiResult.Success(Unit)
        } else {
            ApiResult.Error("Error ${response.code()}: ${response.message()}")
        }
    } catch (e: IOException) {
        ApiResult.Error("Error de conexión: ${e.localizedMessage}", e)
    } catch (e: Exception) {
        ApiResult.Error("Error inesperado: ${e.localizedMessage}", e)
    }
}

suspend fun safeApiCallString(apiCall: suspend () -> Response<String>): ApiResult<String> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            ApiResult.Success(response.body() ?: "")
        } else {
            ApiResult.Error("Error ${response.code()}: ${response.message()}")
        }
    } catch (e: IOException) {
        ApiResult.Error("Error de conexión: ${e.localizedMessage}", e)
    } catch (e: Exception) {
        ApiResult.Error("Error inesperado: ${e.localizedMessage}", e)
    }
}