package com.sanidad.movil.data.remote.dto

data class ResumenPerdidasDTO(
    val totalPerdidasVencimiento: Double,
    val cantidadProductosVencidos: Int,
    val totalInmovilizado: Double,
    val cantidadProductosInmoviles: Int,
    val cantidadInconsistencias: Int
)