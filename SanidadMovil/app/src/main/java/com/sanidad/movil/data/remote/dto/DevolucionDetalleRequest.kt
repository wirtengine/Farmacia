package com.sanidad.movil.data.remote.dto

data class DevolucionDetalleRequest(
    val ventaDetalleId: Long,
    val cantidadDevuelta: Int
)