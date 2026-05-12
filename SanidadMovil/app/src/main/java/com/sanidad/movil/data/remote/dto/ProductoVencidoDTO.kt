package com.sanidad.movil.data.remote.dto

data class ProductoVencidoDTO(
    val medicamentoId: Long,
    val nombre: String,
    val lote: String,
    val fechaVencimiento: String,
    val cantidad: Int,
    val rackNombre: String?
)