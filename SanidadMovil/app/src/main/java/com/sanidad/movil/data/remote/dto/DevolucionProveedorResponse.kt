package com.sanidad.movil.data.remote.dto

data class DevolucionProveedorResponse(
    val id: Long,
    val numeroDevolucion: String,
    val loteId: Long,
    val numeroFacturaLote: String?,
    val proveedorId: Long,
    val proveedorNombre: String,
    val proveedorTelefono: String?,
    val proveedorEmail: String?,
    val solicitadoPorId: Long,
    val solicitadoPorNombre: String,
    val aprobadoPorId: Long?,
    val aprobadoPorNombre: String?,
    val estado: String,
    val motivo: String?,
    val fechaSolicitud: String,
    val fechaAprobacion: String?,
    val detalles: List<DevolucionProveedorDetalleResponse>
)