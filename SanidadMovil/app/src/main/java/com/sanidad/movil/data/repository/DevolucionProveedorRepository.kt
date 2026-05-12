package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class DevolucionProveedorRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun solicitarDevolucion(request: DevolucionProveedorRequest): Result<DevolucionProveedorResponse> = runCatchingApiCall {
        api.solicitarDevolucionProveedor(request)
    }

    suspend fun aprobarDevolucion(request: DevolucionProveedorAprobarRequest): Result<DevolucionProveedorResponse> = runCatchingApiCall {
        api.aprobarDevolucionProveedor(request)
    }

    suspend fun getDevoluciones(): Result<List<DevolucionProveedorResponse>> = runCatchingApiCall {
        api.obtenerDevolucionesProveedor()
    }

    suspend fun getDevolucion(id: Long): Result<DevolucionProveedorResponse> = runCatchingApiCall {
        api.obtenerDevolucionProveedor(id)
    }
}