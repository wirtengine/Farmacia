package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.ApiResult

class RecetaRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun validarReceta(recetaId: Long, aprobar: Boolean, farmaceuticoId: Long?): ApiResult<RecetaResponse> = safeApiCall {
        api.validarReceta(recetaId, aprobar, farmaceuticoId)
    }

    suspend fun getReceta(id: Long): ApiResult<RecetaResponse> = safeApiCall {
        api.obtenerReceta(id)
    }

    suspend fun getRecetasPendientes(): ApiResult<List<RecetaResponse>> = safeApiCall {
        api.obtenerRecetasPendientes()
    }

    suspend fun getRecetasPorFarmaceutico(id: Long): ApiResult<List<RecetaResponse>> = safeApiCall {
        api.obtenerRecetasPorFarmaceutico(id)
    }

    suspend fun getRecetasDisponibles(): ApiResult<List<RecetaResponse>> = safeApiCall {
        api.obtenerRecetasDisponibles()
    }

    suspend fun getTodasLasRecetas(): ApiResult<List<RecetaResponse>> = safeApiCall {
        api.obtenerTodasLasRecetas()
    }
}