package com.sanidad.movil.data.remote.dto

data class VentaDetalleResponse(
    val id: Long,
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val presentacion: String,
    val loteNumero: String,
    val cantidad: Int,
    val precioUnitario: Double,
    val subtotal: Double
)