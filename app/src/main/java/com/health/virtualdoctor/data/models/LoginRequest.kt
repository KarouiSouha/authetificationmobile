package com.health.virtualdoctor.data.models

data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)