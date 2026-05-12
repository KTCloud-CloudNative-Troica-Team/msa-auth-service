package dev.ktcloud.black.auth.adapter.infrastructure.redis

import dev.ktcloud.black.auth.application.port.cache.outbound.AuthCacheCommandOutbound
import dev.ktcloud.black.auth.application.service.jwt.JwtResolver
import dev.ktcloud.black.auth.domain.exception.AuthException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.Date
import java.util.concurrent.TimeUnit

@Component
class AuthCacheRedisCommandAdapter(
    private val redisTemplate: RedisTemplate<String, String>,
    private val jwtResolver: JwtResolver
): AuthCacheCommandOutbound {
    companion object {
        private const val EXPIRED_TIME_PADDING_MILLISECONDS = 500
    }

    override fun deleteRefreshToken(userId: String) {
        redisTemplate.delete(AuthRefreshTokenRedisKey(userId).toRedisKey())
    }

    override fun saveRefreshToken(userId: String, refreshToken: String) {
        val diff = jwtResolver.extractClaims(refreshToken).expiration.time - Date().time

        if (diff <= EXPIRED_TIME_PADDING_MILLISECONDS)
            throw AuthException.ExpiredRefreshToken()

        redisTemplate.opsForValue().set(
            AuthRefreshTokenRedisKey(userId).toRedisKey(),
            refreshToken,
            diff,
            TimeUnit.MILLISECONDS
        )
    }
}