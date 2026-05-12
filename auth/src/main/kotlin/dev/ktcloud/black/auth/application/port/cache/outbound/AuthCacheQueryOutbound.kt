package dev.ktcloud.black.auth.application.port.cache.outbound

interface AuthCacheQueryOutbound {
    fun getRefreshToken(userId: String): String
}