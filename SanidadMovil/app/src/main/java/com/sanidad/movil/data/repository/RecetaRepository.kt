package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class RecetaRepository(private val api: ApiService = NetworkModule.apiService) {

    // Para subir archivos se necesita Multipart; no incluido en esta versión.
    // Puedes implementar cuando se requiera.

    suspend fun validarReceta(recetaId: Long, aprobar: Boolean, farmaceuticoId: Long?): Result<RecetaResponse> = runCatchingApiCall {
        api.validarReceta(recetaId, aprobar, farmaceuticoId)
    }

    suspend fun getReceta(id: Long): Result<RecetaResponse> = runCatchingApiCall {
        api.obtenerReceta(id)
    }

    suspend fun getRecetasPendientes(): Result<List<RecetaResponse>> = runCatchingApiCall {
        api.obtenerRecetasPendientes()
    }

    suspend fun getRecetasPorFarmaceutico(id: Long): Result<List<RecetaResponse>> = runCatchingApiCall {
        api.obtenerRecetasPorFarmaceutico(id)
    }

    suspend fun getRecetasDisponibles(): Result<List<RecetaResponse>> = runCatchingApiCall {
        api.obtenerRecetasDisponibles()
    }

    suspend fun getTodasLasRecetas(): Result<List<RecetaResponse>> = runCatchingApiCall {
        api.obtenerTodasLasRecetas()
    }
}
