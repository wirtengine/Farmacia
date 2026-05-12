package com.sanidad.movil.data.remote.dto

data class UbicacionLoteRequest(
    val loteDetalleId: Long,
    val rackId: Long,
    val nivel: Int,
    val columna: Int,
    val profundidadIndex: Int,
    val cantidad: Int
)