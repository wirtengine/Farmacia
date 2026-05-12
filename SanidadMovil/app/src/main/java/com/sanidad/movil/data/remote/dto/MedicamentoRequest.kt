package com.sanidad.movil.data.remote.dto

data class MedicamentoRequest(
    val registroSanitario: String,
    val nombre: String,
    val presentacion: String,
    val via: String,
    val fabricante: String? = null,
    val tipoVenta: String? = null,
    val precioUnitario: Double,
    val receta: Boolean? = false
)