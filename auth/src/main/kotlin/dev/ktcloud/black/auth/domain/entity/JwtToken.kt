package dev.ktcloud.black.auth.domain.entity

data class JwtToken(
    val accessToken: String,
    val refreshToken: String
)