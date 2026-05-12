package com.sanidad.movil.data.remote.dto

data class ClienteResponse(
    val id: Long,
    val cedula: String,
    val nombre: String,
    val telefono: String?,
    val email: String?,
    val saldo: Double,
    val activo: Boolean
)