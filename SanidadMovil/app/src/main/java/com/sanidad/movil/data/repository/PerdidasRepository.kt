package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.ApiResult

class PerdidasRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getProductosVencidos(): ApiResult<List<ProductoVencidoDTO>> = safeApiCall {
        api.obtenerProductosVencidos()
    }

    suspend fun getProductosInmoviles(): ApiResult<List<ProductoInmovilDTO>> = safeApiCall {
        api.obtenerProductosInmoviles()
    }

    suspend fun getInconsistenciasStock(): ApiResult<List<InconsistenciaStockDTO>> = safeApiCall {
        api.obtenerInconsistenciasStock()
    }

    suspend fun getResumenPerdidas(): ApiResult<ResumenPerdidasDTO> = safeApiCall {
        api.obtenerResumenPerdidas()
    }
}