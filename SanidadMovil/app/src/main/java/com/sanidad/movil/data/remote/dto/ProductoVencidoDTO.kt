package com.sanidad.movil.data.remote.dto

data class ProductoVencidoDTO(
    val loteId: Long,
    val numeroLote: String,
    val fechaVencimiento: String,
    val medicamentoId: Long,
    val medicamentoNombre: String,
    val cantidadVencida: Int,
    val valorPerdido: Double
)