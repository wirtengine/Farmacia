package com.sanidad.movil.data.remote.dto

data class VentaResponse(
    val id: Long,
    val numeroFactura: String,
    val fecha: String, // ISO LocalDateTime
    val clienteId: Long?,
    val clienteNombre: String?,
    val clienteCedula: String?,
    val usuarioId: Long,
    val usuarioUsername: String,
    val subtotal: Double,
    val iva: Double,
    val total: Double,
    val tipo: String,
    val montoUsadoSaldo: Double,
    val montoEfectivo: Double,
    val cambio: Double,
    val detalles: List<VentaDetalleResponse>
)