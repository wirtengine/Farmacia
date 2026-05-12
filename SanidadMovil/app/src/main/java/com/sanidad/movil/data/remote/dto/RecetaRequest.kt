package com.sanidad.movil.data.remote.dto

// No se necesita un request body, se usan parámetros en la URL
data class RecetaRequest(
    val codigoMinsa: String,
    val farmaceuticoId: Long
)