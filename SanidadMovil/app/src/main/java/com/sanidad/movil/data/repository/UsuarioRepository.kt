package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.runCatchingApiCall
import com.sanidad.movil.data.remote.runCatchingApiCallUnit
import com.sanidad.movil.data.remote.runCatchingString

class UsuarioRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun crearUsuario(request: UsuarioRequest): Result<String> = runCatchingString {
        api.crearUsuario(request)
    }

    suspend fun getUsuarios(): Result<List<UsuarioResponse>> = runCatchingApiCall {
        api.obtenerUsuarios()
    }

    suspend fun getUsuario(id: Long): Result<UsuarioResponse> = runCatchingApiCall {
        api.obtenerUsuario(id)
    }

    suspend fun actualizarUsuario(
        id: Long,
        request: ActualizarUsuarioRequest
    ): Result<String> = runCatchingString {
        api.actualizarUsuario(id, request)
    }
}