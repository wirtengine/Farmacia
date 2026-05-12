package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.AlertResponse
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class AlertRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getAlertas(): Result<List<AlertResponse>> = runCatchingApiCall {
        api.obtenerAlertas()
    }

    suspend fun reconocerAlerta(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.reconocerAlerta(id)
    }

    suspend fun generarAlertas(): Result<Unit> = runCatchingApiCallUnit {
        api.generarAlertas()
    }
}