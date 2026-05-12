package com.sanidad.movil.data.remote.dto

data class DevolucionAprobarRequest(
    val devolucionId: Long,
    val aprobadoPorId: Long,
    val aprobada: Boolean,
    val motivoRechazo: String? = null
)