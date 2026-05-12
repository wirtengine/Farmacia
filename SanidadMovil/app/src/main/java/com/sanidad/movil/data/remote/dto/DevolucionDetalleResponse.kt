package com.sanidad.movil.data.remote.dto

data class DevolucionDetalleResponse(
    val id: Long,
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val loteNumero: String,
    val cantidadDevuelta: Int,
    val precioUnitario: Double,
    val subtotal: Double
)