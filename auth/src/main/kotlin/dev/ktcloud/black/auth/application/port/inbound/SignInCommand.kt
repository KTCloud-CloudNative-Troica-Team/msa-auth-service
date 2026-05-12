package dev.ktcloud.black.auth.application.port.inbound

import dev.ktcloud.black.auth.application.dto.JwtTokenDto
import dev.ktcloud.black.user.application.dto.UserDto

interface SignInCommand {
    fun signIn(command: In): Out

    data class In(
        val email: String,
        val password: String,
    )

    data class Out(
        val token: JwtTokenDto,
        val user: UserDto,
    )
}