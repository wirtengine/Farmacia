package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall

class DevolucionProveedorRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun solicitarDevolucion(request: DevolucionProveedorRequest): ApiResult<DevolucionProveedorResponse> = safeApiCall {
        api.solicitarDevolucionProveedor(request)
    }

    suspend fun aprobarDevolucion(request: DevolucionProveedorAprobarRequest): ApiResult<DevolucionProveedorResponse> = safeApiCall {
        api.aprobarDevolucionProveedor(request)
    }

    suspend fun getDevoluciones(): ApiResult<List<DevolucionProveedorResponse>> = safeApiCall {
        api.obtenerDevolucionesProveedor()
    }

    suspend fun getDevolucion(id: Long): ApiResult<DevolucionProveedorResponse> = safeApiCall {
        api.obtenerDevolucionProveedor(id)
    }
}