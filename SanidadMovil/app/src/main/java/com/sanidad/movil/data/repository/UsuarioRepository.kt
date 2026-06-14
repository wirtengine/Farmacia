package com.sanidad.movil.data.repository

import com.sanidad.movil.data.remote.NetworkModule
import com.sanidad.movil.data.remote.api.ApiService
import com.sanidad.movil.data.remote.dto.*
import com.sanidad.movil.data.remote.safeApiCall
import com.sanidad.movil.data.remote.safeApiCallString

class UsuarioRepository(private val api: ApiService = NetworkModule.apiService) {

    suspend fun crearUsuario(request: UsuarioRequest): ApiResult<String> = safeApiCallString {
        api.crearUsuario(request)
    }

    suspend fun getUsuarios(): ApiResult<List<UsuarioResponse>> = safeApiCall {
        api.obtenerUsuarios()
    }

    suspend fun getUsuario(id: Long): ApiResult<UsuarioResponse> = safeApiCall {
        api.obtenerUsuario(id)
    }

    suspend fun actualizarUsuario(id: Long, request: ActualizarUsuarioRequest): ApiResult<String> = safeApiCallString {
        api.actualizarUsuario(id, request)
    }
}