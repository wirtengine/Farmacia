package com.sanidad.movil.data.remote.dto

data class SugerenciaProductoDTO(
    val medicamentoId: Long,
    val nombre: String,
    val presentacion: String,
    val precioUnitario: Double,
    val tipoSugerencia: String,
    val mensaje: String,
    val loteDetalleId: Long?,
    val cantidadSugerida: Int?
)