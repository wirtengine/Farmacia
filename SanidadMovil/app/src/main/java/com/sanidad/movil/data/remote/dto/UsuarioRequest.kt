package com.sanidad.movil.data.remote.dto

data class UsuarioRequest(
    val username: String,
    val password: String,
    val rol: String // "ADMIN" o "VENDEDOR"
)