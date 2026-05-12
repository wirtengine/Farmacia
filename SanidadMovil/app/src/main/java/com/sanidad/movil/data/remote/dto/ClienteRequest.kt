package com.sanidad.movil.data.remote.dto

data class ClienteRequest(
    val cedula: String,
    val nombre: String,
    val telefono: String? = null,
    val email: String? = null,
    val saldo: Double? = null
)