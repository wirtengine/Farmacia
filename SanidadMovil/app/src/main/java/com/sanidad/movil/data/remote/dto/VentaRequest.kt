package com.sanidad.movil.data.remote.dto

data class VentaRequest(
    val clienteId: Long? = null,
    val usuarioId: Long,
    val detalles: List<VentaDetalleRequest>,
    val recetaId: Long? = null,
    val montoUsadoSaldo: Double? = null,
    val montoEfectivo: Double? = null
)