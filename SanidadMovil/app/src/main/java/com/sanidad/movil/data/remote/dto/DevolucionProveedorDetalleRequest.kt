package com.sanidad.movil.data.remote.dto

data class DevolucionProveedorDetalleRequest(
    val loteDetalleId: Long,
    val cantidadDevuelta: Int
)