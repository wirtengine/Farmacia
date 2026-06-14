package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult

class RecommendationRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getRecomendaciones(): ApiResult<List<RecommendationResponse>> = safeApiCall {
        api.obtenerRecomendaciones()
    }

    suspend fun aceptarRecomendacion(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.aceptarRecomendacion(id)
    }

    suspend fun descartarRecomendacion(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.descartarRecomendacion(id)
    }

    suspend fun generarRecomendaciones(): ApiResult<Unit> = safeApiCallUnit {
        api.generarRecomendaciones()
    }
}