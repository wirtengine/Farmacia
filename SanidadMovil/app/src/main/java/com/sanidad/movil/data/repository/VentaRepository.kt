package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult

class VentaRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getVentas(): ApiResult<List<VentaResponse>> = safeApiCall {
        api.obtenerVentas()
    }

    suspend fun getVenta(id: Long): ApiResult<VentaResponse> = safeApiCall {
        api.obtenerVenta(id)
    }

    suspend fun crearVenta(request: VentaRequest): ApiResult<VentaResponse> = safeApiCall {
        api.crearVenta(request)
    }

    suspend fun anularVenta(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.anularVenta(id)
    }
}