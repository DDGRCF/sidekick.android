package dev.ddgrcf.sidekick.core.relay

import dev.ddgrcf.sidekick.core.store.SecretStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TokenManager(
    private val api: RelayApi,
    private val secretStore: SecretStore,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val mutex = Mutex()
    private val _logoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val logoutEvents: SharedFlow<Unit> = _logoutEvents.asSharedFlow()

    suspend fun validAccessToken(): String {
        val cur = secretStore.current()
        if (cur.accessToken.isNotEmpty() && cur.accessExpiresAtMs - nowMs() > 60_000L) {
            return cur.accessToken
        }
        return forceRefresh()
    }

    suspend fun forceRefresh(): String = mutex.withLock {
        val cur = secretStore.current()
        if (cur.refreshToken.isBlank() || cur.relayUrl.isBlank()) {
            secretStore.clearAuth()
            _logoutEvents.tryEmit(Unit)
            throw AuthExpiredException("missing refresh token")
        }
        try {
            val next = api.refresh(cur.relayUrl, cur.refreshToken)
            secretStore.updateTokens(
                accessToken = next.access_token,
                expiresAtMs = next.expires_at_ms,
                refreshToken = next.refresh_token,
                userId = next.user_id,
                deviceId = next.device_id,
            )
            return next.access_token
        } catch (e: AuthExpiredException) {
            secretStore.clearAuth()
            _logoutEvents.tryEmit(Unit)
            throw e
        }
    }

    suspend fun <T> withValidToken(block: suspend (relayUrl: String, accessToken: String) -> T): T {
        val relayUrl = secretStore.current().relayUrl
        val token = validAccessToken()
        return try {
            block(relayUrl, token)
        } catch (e: RelayHttpException) {
            if (e.statusCode == 401) {
                val refreshed = forceRefresh()
                block(relayUrl, refreshed)
            } else {
                throw e
            }
        }
    }
}
