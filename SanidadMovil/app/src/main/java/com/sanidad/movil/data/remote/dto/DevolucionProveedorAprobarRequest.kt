package com.sanidad.movil.data.remote.dto

data class DevolucionProveedorAprobarRequest(
    val devolucionId: Long,
    val aprobadoPorId: Long,
    val aprobada: Boolean,
    val motivoRechazo: String? = null
)