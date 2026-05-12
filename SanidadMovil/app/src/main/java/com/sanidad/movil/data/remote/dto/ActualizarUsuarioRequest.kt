package com.sanidad.movil.data.remote.dto

data class ActualizarUsuarioRequest(
    val password: String? = null,
    val rol: String
)