package com.sanidad.movil.data.remote.dto

data class ProveedorResponse(
    val id: Long,
    val ruc: String,
    val nombre: String,
    val telefono: String?,
    val email: String?,
    val activo: Boolean
)