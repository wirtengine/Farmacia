package com.sanidad.movil.data.remote.dto

data class RecommendationResponse(
    val id: Long,
    val type: String,
    val priority: String,
    val title: String,
    val description: String,
    val suggestedAction: String?,
    val relatedEntityId: Long?,
    val relatedEntityType: String?,
    val createdAt: String,
    val respondedAt: String?,
    val status: String
)