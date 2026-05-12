package com.sanidad.movil.data.remote.dto

data class DevolucionProveedorDetalleResponse(
    val id: Long,
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val loteNumero: String,
    val cantidadDevuelta: Int
)