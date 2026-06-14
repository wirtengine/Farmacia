package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.AlertResponse
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult


class AlertRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getAlertas(): ApiResult<List<AlertResponse>> = safeApiCall {
        api.obtenerAlertas()
    }

    suspend fun reconocerAlerta(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.reconocerAlerta(id)
    }

    suspend fun generarAlertas(): ApiResult<Unit> = safeApiCallUnit {
        api.generarAlertas()
    }
}