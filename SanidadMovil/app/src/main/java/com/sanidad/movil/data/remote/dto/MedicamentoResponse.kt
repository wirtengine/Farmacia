package com.sanidad.movil.data.remote.dto

data class MedicamentoResponse(
    val id: Long,
    val registroSanitario: String,
    val nombre: String,
    val presentacion: String,
    val via: String,
    val fabricante: String,
    val tipoVenta: String,
    val precioUnitario: Double,
    val receta: Boolean,
    val activo: Boolean,
    val imagen: String?
)