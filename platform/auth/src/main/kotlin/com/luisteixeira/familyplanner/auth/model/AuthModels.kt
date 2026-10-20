package com.luisteixeira.familyplanner.auth.model

import java.time.Instant
import java.util.UUID

data class User(
    val id: UUID,
    val email: String,
    val displayName: String,
    val createdAt: Instant,
    val passwordHash: String,
)

data class Session(
    val token: String,
    val userId: UUID,
    val expiresAt: Instant,
)

data class AuthResult(
    val user: User,
    val session: Session,
)
