package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class PerdidasRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getProductosVencidos(): Result<List<ProductoVencidoDTO>> = runCatchingApiCall {
        api.obtenerProductosVencidos()
    }

    suspend fun getProductosInmoviles(): Result<List<ProductoInmovilDTO>> = runCatchingApiCall {
        api.obtenerProductosInmoviles()
    }

    suspend fun getInconsistenciasStock(): Result<List<InconsistenciaStockDTO>> = runCatchingApiCall {
        api.obtenerInconsistenciasStock()
    }

    suspend fun getResumenPerdidas(): Result<ResumenPerdidasDTO> = runCatchingApiCall {
        api.obtenerResumenPerdidas()
    }
}