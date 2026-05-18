package com.sanidad.movil.data

object UserSession {
    var userId: Long = 0L
    var username: String = ""
    var rol: String = ""

    fun isAdmin() = rol == "ADMIN"
}