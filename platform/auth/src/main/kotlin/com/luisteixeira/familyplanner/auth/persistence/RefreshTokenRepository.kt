package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.RefreshToken
import java.util.UUID
import java.time.Instant

interface RefreshTokenRepository {
    suspend fun saveToken(token: RefreshToken)
    suspend fun findByHash(tokenHash: String): RefreshToken?
    suspend fun revokeIfActive(tokenHash: String, at: Instant): Boolean
    suspend fun revokeFamily(familyId: UUID, at: Instant)
}