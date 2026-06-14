package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult

class LoteRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getLotes(): ApiResult<List<LoteResponse>> = safeApiCall {
        api.obtenerLotes()
    }

    suspend fun getLote(id: Long): ApiResult<LoteResponse> = safeApiCall {
        api.obtenerLote(id)
    }

    suspend fun crearLote(request: LoteRequest): ApiResult<LoteResponse> = safeApiCall {
        api.crearLote(request)
    }

    suspend fun actualizarLote(id: Long, request: LoteRequest): ApiResult<LoteResponse> = safeApiCall {
        api.actualizarLote(id, request)
    }

    suspend fun suspenderLote(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.suspenderLote(id)
    }
}