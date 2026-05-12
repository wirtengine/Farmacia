package com.sanidad.movil.data.remote.dto

data class InconsistenciaStockDTO(
    val medicamentoId: Long,
    val nombre: String,
    val stockSistema: Int,
    val stockReal: Int,
    val diferencia: Int
)