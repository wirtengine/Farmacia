package com.sanidad.movil.data.remote

import retrofit2.Response

suspend fun <T> runCatchingApiCall(block: suspend () -> Response<T>): Result<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

suspend fun runCatchingApiCallUnit(block: suspend () -> Response<Void>): Result<Unit> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Para endpoints que devuelven String (como crearUsuario)
suspend fun runCatchingString(block: suspend () -> Response<String>): Result<String> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            Result.success(response.body() ?: "")
        } else {
            Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}