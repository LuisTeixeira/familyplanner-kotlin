package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.RefreshToken
import java.time.Instant
import java.util.UUID

class InMemoryRefreshTokenRepository : RefreshTokenRepository {
    private val lock = Any()
    private val tokens = HashMap<String, RefreshToken>()

    override suspend fun saveToken(token: RefreshToken) {
        synchronized(lock) {
            tokens[token.tokenHash] = token
        }
    }

    override suspend fun findByHash(tokenHash: String): RefreshToken? {
        return synchronized(lock) {
            tokens[tokenHash]
        }
    }

    override suspend fun revokeIfActive(tokenHash: String, at: Instant): Boolean =
        synchronized(lock) {
            val token = tokens[tokenHash] ?: return@synchronized false
            if (token.revokedAt != null) return@synchronized false
            tokens[tokenHash] = token.copy(revokedAt = at)
            true
        }

    override suspend fun revokeFamily(familyId: UUID, at: Instant) {
        synchronized(lock) {
            tokens.replaceAll { _, token ->
                if (token.familyId == familyId && token.revokedAt == null) token.copy(revokedAt = at) else token
            }
        }
    }
}