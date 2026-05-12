package com.sanidad.movil.data.remote.dto

data class StockMedicamentoDTO(
    val medicamentoId: Long,
    val nombre: String,
    val stockTotal: Int
)