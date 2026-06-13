package com.sanidad.movil.data.remote.dto

data class ProductoInmovilDTO(
    val medicamentoId: Long,
    val medicamentoNombre: String,
    val stockActual: Int,
    val ventasUltimos90Dias: Int,
    val diasSinMovimiento: Int,
    val valorInmovilizado: Double
)