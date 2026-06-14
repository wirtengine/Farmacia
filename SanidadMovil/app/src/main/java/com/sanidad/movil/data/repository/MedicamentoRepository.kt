package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.safeApiCallString

class MedicamentoRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getMedicamentos(): ApiResult<List<MedicamentoResponse>> = safeApiCall {
        api.obtenerMedicamentos()
    }

    suspend fun getMedicamento(id: Long): ApiResult<MedicamentoResponse> = safeApiCall {
        api.obtenerMedicamento(id)
    }

    suspend fun crearMedicamento(request: MedicamentoRequest): ApiResult<MedicamentoResponse> = safeApiCall {
        api.crearMedicamento(request)
    }

    suspend fun actualizarMedicamento(id: Long, request: MedicamentoRequest): ApiResult<MedicamentoResponse> = safeApiCall {
        api.actualizarMedicamento(id, request)
    }

    suspend fun desactivarMedicamento(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.desactivarMedicamento(id)
    }

    suspend fun activarMedicamento(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.activarMedicamento(id)
    }

    // Métodos adicionales que ya estaban en ApiService
    suspend fun subirImagen(id: Long, file: okhttp3.MultipartBody.Part): ApiResult<String> = safeApiCallString {
        api.subirImagenMedicamento(id, file)
    }

    suspend fun obtenerStockMedicamento(id: Long): ApiResult<StockMedicamentoDTO> = safeApiCall {
        api.obtenerStockMedicamento(id)
    }

    suspend fun obtenerLotesMedicamento(id: Long): ApiResult<LotesMedicamentoDTO> = safeApiCall {
        api.obtenerLotesMedicamento(id)
    }
}