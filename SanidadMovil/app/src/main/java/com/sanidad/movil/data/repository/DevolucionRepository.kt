package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall

class DevolucionRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun solicitarDevolucion(request: DevolucionRequest): ApiResult<DevolucionResponse> = safeApiCall {
        api.solicitarDevolucion(request)
    }

    suspend fun aprobarDevolucion(request: DevolucionAprobarRequest): ApiResult<DevolucionResponse> = safeApiCall {
        api.aprobarDevolucion(request)
    }

    suspend fun getDevoluciones(): ApiResult<List<DevolucionResponse>> = safeApiCall {
        api.obtenerDevoluciones()
    }

    suspend fun getDevolucion(id: Long): ApiResult<DevolucionResponse> = safeApiCall {
        api.obtenerDevolucion(id)
    }
}