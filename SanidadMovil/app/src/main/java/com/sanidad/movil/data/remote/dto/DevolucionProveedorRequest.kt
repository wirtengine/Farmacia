package com.sanidad.movil.data.remote.dto

data class DevolucionProveedorRequest(
    val loteId: Long,
    val solicitadoPorId: Long,
    val motivo: String? = null,
    val detalles: List<DevolucionProveedorDetalleRequest>
)