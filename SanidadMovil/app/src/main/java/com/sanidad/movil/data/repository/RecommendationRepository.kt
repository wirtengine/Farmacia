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

    suspend fun aceptarRecomendacion(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.aceptarRecomendacion(id)
    }

    suspend fun descartarRecomendacion(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.descartarRecomendacion(id)
    }

    suspend fun generarRecomendaciones(): Result<Unit> = runCatchingApiCallUnit {
        api.generarRecomendaciones()
    }
}