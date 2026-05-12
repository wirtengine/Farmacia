package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class DevolucionRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun solicitarDevolucion(request: DevolucionRequest): Result<DevolucionResponse> = runCatchingApiCall {
        api.solicitarDevolucion(request)
    }

    suspend fun aprobarDevolucion(request: DevolucionAprobarRequest): Result<DevolucionResponse> = runCatchingApiCall {
        api.aprobarDevolucion(request)
    }

    suspend fun getDevoluciones(): Result<List<DevolucionResponse>> = runCatchingApiCall {
        api.obtenerDevoluciones()
    }

    suspend fun getDevolucion(id: Long): Result<DevolucionResponse> = runCatchingApiCall {
        api.obtenerDevolucion(id)
    }
}