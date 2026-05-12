package com.sanidad.movil.data.remote.dto

data class LoteRequest(
    val fechaFabricacion: String? = null, // ISO date
    val fechaVencimiento: String? = null,
    val proveedorId: Long,
    val factura: String? = null,
    val detalles: List<LoteDetalleRequest>
)