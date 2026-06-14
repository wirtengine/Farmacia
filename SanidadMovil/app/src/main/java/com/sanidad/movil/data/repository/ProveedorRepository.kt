package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult

class ProveedorRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getProveedores(): ApiResult<List<ProveedorResponse>> = safeApiCall {
        api.obtenerProveedores()
    }

    suspend fun getProveedor(id: Long): ApiResult<ProveedorResponse> = safeApiCall {
        api.obtenerProveedor(id)
    }

    suspend fun crearProveedor(request: ProveedorRequest): ApiResult<ProveedorResponse> = safeApiCall {
        api.crearProveedor(request)
    }

    suspend fun actualizarProveedor(id: Long, request: ProveedorRequest): ApiResult<ProveedorResponse> = safeApiCall {
        api.actualizarProveedor(id, request)
    }

    suspend fun suspenderProveedor(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.suspenderProveedor(id)
    }
}