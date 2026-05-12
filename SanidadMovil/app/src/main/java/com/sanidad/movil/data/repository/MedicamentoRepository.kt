package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class MedicamentoRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getMedicamentos(): Result<List<MedicamentoResponse>> = runCatchingApiCall {
        api.obtenerMedicamentos()
    }

    suspend fun getMedicamento(id: Long): Result<MedicamentoResponse> = runCatchingApiCall {
        api.obtenerMedicamento(id)
    }

    suspend fun crearMedicamento(request: MedicamentoRequest): Result<MedicamentoResponse> = runCatchingApiCall {
        api.crearMedicamento(request)
    }

    suspend fun actualizarMedicamento(id: Long, request: MedicamentoRequest): Result<MedicamentoResponse> = runCatchingApiCall {
        api.actualizarMedicamento(id, request)
    }

    suspend fun desactivarMedicamento(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.desactivarMedicamento(id)
    }

    suspend fun activarMedicamento(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.activarMedicamento(id)
    }

    // No incluimos subir imagen ni stock/lotes en esta versión básica; puedes agregarlos si es necesario.
}