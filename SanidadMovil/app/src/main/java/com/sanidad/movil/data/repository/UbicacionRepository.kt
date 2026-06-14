package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult

class UbicacionRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getTodasUbicaciones(): ApiResult<List<UbicacionLoteResponse>> = safeApiCall {
        api.obtenerUbicaciones()
    }

    suspend fun getUbicacionesPorRack(rackId: Long): ApiResult<List<UbicacionLoteResponse>> = safeApiCall {
        api.obtenerUbicacionesPorRack(rackId)
    }

    suspend fun getUbicacion(id: Long): ApiResult<UbicacionLoteResponse> = safeApiCall {
        api.obtenerUbicacion(id)
    }

    suspend fun asignarUbicacion(request: UbicacionLoteRequest): ApiResult<UbicacionLoteResponse> = safeApiCall {
        api.asignarUbicacion(request)
    }

    suspend fun eliminarUbicacion(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.eliminarUbicacion(id)
    }
}