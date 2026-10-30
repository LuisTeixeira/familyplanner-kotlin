package com.luisteixeira.familyplanner.auth.token

import java.util.UUID
import java.time.Instant

data class RefreshToken(
    val tokenHash: String,
    val userId: UUID,
    val familyId: UUID,
    val expiresAt: Instant,
    val revokedAt: Instant?,
)
