package com.sanidad.movil.data.remote.dto

data class UbicacionLoteResponse(
    val id: Long,
    val rackId: Long,
    val rackNombre: String,
    val loteDetalleId: Long,
    val medicamentoNombre: String,
    val nivel: Int,
    val columna: Int,
    val profundidadIndex: Int,
    val cantidad: Int,
    val activo: Boolean
)