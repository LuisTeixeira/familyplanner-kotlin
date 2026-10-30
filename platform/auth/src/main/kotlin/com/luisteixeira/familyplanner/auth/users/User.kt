package com.luisteixeira.familyplanner.auth.users

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val createdAt: Instant,
    val passwordHash: String,
)
