package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class ProveedorRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getProveedores(): Result<List<ProveedorResponse>> = runCatchingApiCall {
        api.obtenerProveedores()
    }

    suspend fun getProveedor(id: Long): Result<ProveedorResponse> = runCatchingApiCall {
        api.obtenerProveedor(id)
    }

    suspend fun crearProveedor(request: ProveedorRequest): Result<ProveedorResponse> = runCatchingApiCall {
        api.crearProveedor(request)
    }

    suspend fun actualizarProveedor(id: Long, request: ProveedorRequest): Result<ProveedorResponse> = runCatchingApiCall {
        api.actualizarProveedor(id, request)
    }

    suspend fun suspenderProveedor(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.suspenderProveedor(id)
    }
}