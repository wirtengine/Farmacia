package com.sanidad.movil.data.remote.dto

data class RecommendationResponse(
    val id: Long,
    val type: String,
    val priority: String,
    val message: String,
    val status: String,
    val createdAt: String
)