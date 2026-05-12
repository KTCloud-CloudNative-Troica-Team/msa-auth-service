package dev.ktcloud.black.auth.adapter.presentation.web.inbound

import dev.ktcloud.black.auth.adapter.presentation.web.inbound.request.SignInRequest
import dev.ktcloud.black.auth.adapter.presentation.web.inbound.request.TokenRefreshRequest
import dev.ktcloud.black.auth.adapter.presentation.web.inbound.response.SignInResponse
import dev.ktcloud.black.auth.adapter.presentation.web.inbound.response.TokenRefreshResponse

interface AuthRestController {
    fun signIn(req: SignInRequest): SignInResponse
    fun tokenRefresh(req: TokenRefreshRequest): TokenRefreshResponse
}