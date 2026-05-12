package dev.ktcloud.black.auth.adapter.presentation.web.inbound.request

data class SignInRequest(
    val email: String,
    val plainPassword: String
)
