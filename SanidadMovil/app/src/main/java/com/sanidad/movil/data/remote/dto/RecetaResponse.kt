package com.sanidad.movil.data.remote.dto

data class RecetaResponse(
    val id: Long,
    val imagenUrl: String?,
    val fechaSubida: String?,
    val estado: String,
    val farmaceuticoId: Long,
    val farmaceuticoUsername: String?,
    val ventaId: Long?,
    val codigoMinsa: String?
)