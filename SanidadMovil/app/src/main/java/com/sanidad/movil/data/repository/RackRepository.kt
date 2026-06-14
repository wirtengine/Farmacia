package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit

class RackRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getRacks(): ApiResult<List<RackResponse>> = safeApiCall {
        api.obtenerRacks()
    }

    suspend fun getRack(id: Long): ApiResult<RackResponse> = safeApiCall {
        api.obtenerRack(id)
    }

    suspend fun crearRack(request: RackRequest): ApiResult<RackResponse> = safeApiCall {
        api.crearRack(request)
    }

    suspend fun actualizarRack(id: Long, request: RackRequest): ApiResult<RackResponse> = safeApiCall {
        api.actualizarRack(id, request)
    }

    suspend fun eliminarRack(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.eliminarRack(id)
    }
}