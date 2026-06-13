package com.sanidad.movil.data.remote.dto

data class InconsistenciaStockDTO(
    val loteDetalleId: Long,
    val medicamentoId: Long,
    val medicamentoNombre: String,
    val cantidadLote: Int,
    val cantidadUbicaciones: Int,
    val diferencia: Int
)