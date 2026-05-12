package dev.ktcloud.black.auth.adapter.presentation.web.inbound.request

data class TokenRefreshRequest(
    val refreshToken: String,
)