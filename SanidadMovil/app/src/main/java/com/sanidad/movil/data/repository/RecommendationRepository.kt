package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.RecommendationResponse
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class RecommendationRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getRecomendaciones(): Result<List<RecommendationResponse>> = runCatchingApiCall {
        api.obtenerRecomendaciones()
    }

    suspend fun aplicarRecomendacion(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.aplicarRecomendacion(id)
    }
}