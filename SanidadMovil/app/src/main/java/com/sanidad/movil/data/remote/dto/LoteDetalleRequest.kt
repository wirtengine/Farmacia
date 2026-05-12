package com.sanidad.movil.data.remote.dto

data class LoteDetalleRequest(
    val medicamentoId: Long,
    val cantidad: Int,
    val rackId: Long? = null,
    val nivel: Int? = null,
    val columna: Int? = null,
    val profundidadIndex: Int? = null
)