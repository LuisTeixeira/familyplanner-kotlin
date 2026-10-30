package com.luisteixeira.familyplanner.auth.model

import java.time.Instant
import java.util.UUID

data class ApiClient(
    val id: UUID,
    val name: String,
    val keyHash: String,
    val createdAt: Instant,
    val revokedAt: Instant?
)