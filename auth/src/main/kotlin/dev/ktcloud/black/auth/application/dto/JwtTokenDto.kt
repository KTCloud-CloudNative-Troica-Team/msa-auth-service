package dev.ktcloud.black.auth.application.dto

import dev.ktcloud.black.auth.domain.entity.JwtToken

data class JwtTokenDto(
    val accessToken: String,
    val refreshToken: String
) {
    companion object {
        fun from(jwtToken: JwtToken): JwtTokenDto {
            return JwtTokenDto(
                accessToken = jwtToken.accessToken,
                refreshToken = jwtToken.refreshToken
            )
        }
    }
}