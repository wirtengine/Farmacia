package com.sanidad.movil.data.remote.dto

data class DevolucionResponse(
    val id: Long,
    val numeroDevolucion: String,
    val ventaId: Long,
    val numeroFactura: String,
    val usuarioSolicitanteId: Long,
    val usuarioSolicitanteNombre: String,
    val usuarioApruebaId: Long?,
    val usuarioApruebaNombre: String?,
    val estado: String,
    val motivo: String?,
    val fechaSolicitud: String,
    val fechaAprobacion: String?,
    val subtotalDevuelto: Double,
    val ivaDevuelto: Double,
    val totalDevuelto: Double,
    val montoDevueltoEfectivo: Double,
    val montoDevueltoSaldo: Double,
    val detalles: List<DevolucionDetalleResponse>
)