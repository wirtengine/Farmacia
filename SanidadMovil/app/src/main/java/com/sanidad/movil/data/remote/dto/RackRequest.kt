package com.sanidad.movil.data.remote.dto

data class RackRequest(
    val nombre: String,
    val descripcion: String? = null,
    val ancho: Int,
    val alto: Int,
    val profundidad: Int
)