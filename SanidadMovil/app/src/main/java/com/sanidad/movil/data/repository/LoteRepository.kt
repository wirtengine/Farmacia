package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class LoteRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getLotes(): Result<List<LoteResponse>> = runCatchingApiCall {
        api.obtenerLotes()
    }

    suspend fun getLote(id: Long): Result<LoteResponse> = runCatchingApiCall {
        api.obtenerLote(id)
    }

    suspend fun crearLote(request: LoteRequest): Result<LoteResponse> = runCatchingApiCall {
        api.crearLote(request)
    }

    suspend fun actualizarLote(id: Long, request: LoteRequest): Result<LoteResponse> = runCatchingApiCall {
        api.actualizarLote(id, request)
    }

    suspend fun suspenderLote(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.suspenderLote(id)
    }
}