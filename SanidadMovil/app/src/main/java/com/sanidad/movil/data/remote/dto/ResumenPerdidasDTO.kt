package com.sanidad.movil.data.remote.dto

data class ResumenPerdidasDTO(
    val totalProductosVencidos: Int,
    val totalProductosInmoviles: Int,
    val totalInconsistencias: Int,
    val perdidaEstimada: Double
)