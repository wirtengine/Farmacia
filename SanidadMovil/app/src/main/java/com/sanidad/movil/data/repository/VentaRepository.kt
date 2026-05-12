package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class VentaRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getVentas(): Result<List<VentaResponse>> = runCatchingApiCall {
        api.obtenerVentas()
    }

    suspend fun getVenta(id: Long): Result<VentaResponse> = runCatchingApiCall {
        api.obtenerVenta(id)
    }

    suspend fun crearVenta(request: VentaRequest): Result<VentaResponse> = runCatchingApiCall {
        api.crearVenta(request)
    }

    suspend fun anularVenta(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.anularVenta(id)
    }
}