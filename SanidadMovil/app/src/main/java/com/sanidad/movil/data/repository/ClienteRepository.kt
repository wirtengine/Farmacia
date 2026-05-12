package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit

class ClienteRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun getClientes(): Result<List<ClienteResponse>> = runCatchingApiCall {
        api.obtenerClientes()
    }

    suspend fun getCliente(id: Long): Result<ClienteResponse> = runCatchingApiCall {
        api.obtenerCliente(id)
    }

    suspend fun crearCliente(request: ClienteRequest): Result<ClienteResponse> = runCatchingApiCall {
        api.crearCliente(request)
    }

    suspend fun actualizarCliente(id: Long, request: ClienteRequest): Result<ClienteResponse> = runCatchingApiCall {
        api.actualizarCliente(id, request)
    }

    suspend fun suspenderCliente(id: Long): Result<Unit> = runCatchingApiCallUnit {
        api.suspenderCliente(id)
    }

    suspend fun abonarSaldo(id: Long, monto: Double): Result<ClienteResponse> = runCatchingApiCall {
        api.abonarSaldo(id, monto)
    }
}