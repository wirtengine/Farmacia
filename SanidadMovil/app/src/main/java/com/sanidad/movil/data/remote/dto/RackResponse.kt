package com.sanidad.movil.data.remote.dto

data class RackResponse(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val ancho: Int,
    val alto: Int,
    val profundidad: Int,
    val activo: Boolean
)