package dev.ktcloud.black.auth.adapter.presentation.web.inbound.response

import dev.ktcloud.black.auth.application.dto.JwtTokenDto
import dev.ktcloud.black.user.application.dto.UserDto

data class SignInResponse(
    val token: JwtTokenDto,
    val user: UserDto,
)