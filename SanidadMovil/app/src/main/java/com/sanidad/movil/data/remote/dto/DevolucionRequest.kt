package com.sanidad.movil.data.remote.dto

data class DevolucionRequest(
    val ventaId: Long,
    val solicitadoPorId: Long,
    val motivo: String? = null,
    val detalles: List<DevolucionDetalleRequest>
)