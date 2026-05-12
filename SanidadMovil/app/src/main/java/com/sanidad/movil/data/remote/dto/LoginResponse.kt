package com.sanidad.movil.data.remote.dto

data class LoginResponse(
    val token: String,
    val id: Long,
    val username: String,
    val rol: String
)