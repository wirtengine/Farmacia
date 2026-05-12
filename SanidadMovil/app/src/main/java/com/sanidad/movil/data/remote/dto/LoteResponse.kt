package com.sanidad.movil.data.remote.dto

data class LoteResponse(
    val id: Long,
    val numeroLote: String,
    val fechaFabricacion: String?,
    val fechaVencimiento: String?,
    val proveedorId: Long,
    val proveedorNombre: String,
    val proveedorRuc: String,
    val factura: String?,
    val activo: Boolean,
    val detalles: List<LoteDetalleResponse>
)