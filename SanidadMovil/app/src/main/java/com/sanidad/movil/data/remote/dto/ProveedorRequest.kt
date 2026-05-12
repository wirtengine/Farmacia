package com.sanidad.movil.data.remote.dto

data class ProveedorRequest(
    val ruc: String,
    val nombre: String,
    val telefono: String? = null,
    val email: String? = null
)