package com.luisteixeira.familyplanner.auth.session

import com.luisteixeira.familyplanner.core.domain.DomainException
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.HexFormat
import java.util.UUID

class SessionService(
    private val repo: SessionRepository,
    private val clock: Clock = Clock.systemUTC(),
) : SessionValidator {

    suspend fun create(userId: UUID): Session {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val session = Session(
            token = HexFormat.of().formatHex(bytes),
            userId = userId,
            expiresAt = clock.instant().plus(Duration.ofDays(7)),
        )
        repo.saveSession(session)
        return session
    }

    override suspend fun validateSession(token: String): UUID {
        val session = repo.findSession(token) ?: throw DomainException("unauthorized")
        if (clock.instant().isAfter(session.expiresAt)) {
            repo.deleteSession(token)
            throw DomainException("session expired")
        }
        return session.userId
    }

    suspend fun delete(token: String) = repo.deleteSession(token)
}
