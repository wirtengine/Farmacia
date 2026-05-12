package com.sanidad.movil.data.remote.dto

data class ProductoInmovilDTO(
    val medicamentoId: Long,
    val nombre: String,
    val lote: String,
    val ultimaVenta: String?,
    val diasInmovil: Int,
    val cantidad: Int
)