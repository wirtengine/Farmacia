package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class UbicacionRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getTodasUbicaciones(): Result<List<UbicacionLoteResponse>> = runCatchingApiCall {
        api.obtenerUbicaciones()
    }

    suspend fun getUbicacionesPorRack(rackId: Long): Result<List<UbicacionLoteResponse>> = runCatchingApiCall {
        api.obtenerUbicacionesPorRack(rackId)
    }

    suspend fun getUbicacion(id: Long): Result<UbicacionLoteResponse> = runCatchingApiCall {
        api.obtenerUbicacion(id)
    }

    suspend fun asignarUbicacion(request: UbicacionLoteRequest): Result<UbicacionLoteResponse> = runCatchingApiCall {
        api.asignarUbicacion(request)
    }

    suspend fun eliminarUbicacion(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.eliminarUbicacion(id)
    }
}