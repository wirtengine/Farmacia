package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallUnit
import com.sanidad.movil.data.remote.ApiResult

class ClienteRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getClientes(): ApiResult<List<ClienteResponse>> = safeApiCall {
        api.obtenerClientes()
    }

    suspend fun getCliente(id: Long): ApiResult<ClienteResponse> = safeApiCall {
        api.obtenerCliente(id)
    }

    suspend fun crearCliente(request: ClienteRequest): ApiResult<ClienteResponse> = safeApiCall {
        api.crearCliente(request)
    }

    suspend fun actualizarCliente(id: Long, request: ClienteRequest): ApiResult<ClienteResponse> = safeApiCall {
        api.actualizarCliente(id, request)
    }

    suspend fun suspenderCliente(id: Long): ApiResult<Unit> = safeApiCallUnit {
        api.suspenderCliente(id)
    }

    suspend fun abonarSaldo(id: Long, monto: Double): ApiResult<ClienteResponse> = safeApiCall {
        api.abonarSaldo(id, monto)
    }
}