package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.DashboardResponseDTO
import com.sanidad.movil.data.remote.safeApiCall

class DashboardRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun obtenerDashboard(): ApiResult<DashboardResponseDTO> = safeApiCall {
        api.obtenerDashboard()
    }
}