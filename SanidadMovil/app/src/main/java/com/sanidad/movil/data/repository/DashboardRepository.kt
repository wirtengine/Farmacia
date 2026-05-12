package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.DashboardResponseDTO
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class DashboardRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun obtenerDashboard(): Result<DashboardResponseDTO> = runCatchingApiCall {
        api.obtenerDashboard()
    }
}