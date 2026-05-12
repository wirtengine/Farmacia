package com.sanidad.movil.data.remote.dto

data class LoteDetalleResponse(
    val id: Long,
    val medicamentoId: Long,
    val medicamentoNombre: String,
    val medicamentoPresentacion: String,
    val fabricante: String,
    val cantidad: Int,
    val precioUnitario: Double
)