package dev.ktcloud.black.auth.application.port.cache.outbound

interface AuthCacheCommandOutbound {
    fun deleteRefreshToken(userId: String)
    fun saveRefreshToken(userId: String, refreshToken: String)
}