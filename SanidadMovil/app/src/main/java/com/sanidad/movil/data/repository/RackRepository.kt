package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class RackRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getRacks(): Result<List<RackResponse>> = runCatchingApiCall {
        api.obtenerRacks()
    }

    suspend fun getRack(id: Long): Result<RackResponse> = runCatchingApiCall {
        api.obtenerRack(id)
    }

    suspend fun crearRack(request: RackRequest): Result<RackResponse> = runCatchingApiCall {
        api.crearRack(request)
    }

    suspend fun actualizarRack(id: Long, request: RackRequest): Result<RackResponse> = runCatchingApiCall {
        api.actualizarRack(id, request)
    }

    suspend fun eliminarRack(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.eliminarRack(id)
    }
}