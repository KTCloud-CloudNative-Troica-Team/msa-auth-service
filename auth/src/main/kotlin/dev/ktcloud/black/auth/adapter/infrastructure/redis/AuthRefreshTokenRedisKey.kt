package dev.ktcloud.black.auth.adapter.infrastructure.redis

data class AuthRefreshTokenRedisKey(
    val userId: String,
) {
    fun toRedisKey(): String = "auth-refresh-token:$userId"

    override fun toString(): String = toRedisKey()
}