package com.sanidad.movil.data.remote.dto

data class AlertResponse(
    val id: Long,
    val type: String,
    val severity: String,
    val message: String,
    val status: String,
    val createdAt: String,
    val acknowledgedAt: String?
)