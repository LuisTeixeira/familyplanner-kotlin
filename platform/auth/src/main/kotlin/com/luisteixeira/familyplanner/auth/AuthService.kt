package com.luisteixeira.familyplanner.auth

import com.luisteixeira.familyplanner.auth.model.AuthResult
import com.luisteixeira.familyplanner.auth.model.Session
import com.luisteixeira.familyplanner.auth.model.User
import com.luisteixeira.familyplanner.auth.persistence.AuthRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.HexFormat
import java.util.UUID

class AuthService(
    private val repo: AuthRepository,
    private val clock: Clock = Clock.systemUTC(),
) : SessionValidator, UserDirectory {

    suspend fun register(email: String, password: String, displayName: String): AuthResult {
        if (email.isEmpty() || password.isEmpty()) throw DomainException("email and password are required")

        val user = User(
            id = UUID.randomUUID(),
            email = email,
            displayName = displayName,
            createdAt = clock.instant(),
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
        )
        try {
            repo.saveUser(user)
        } catch (e: DuplicateEntryException) {
            throw DomainException("user already exists")
        }
        return AuthResult(user, createSession(user.id))
    }

    suspend fun login(email: String, password: String): AuthResult {
        val user = repo.findUserByEmail(email) ?: throw DomainException("invalid credentials")
        if (!BCrypt.checkpw(password, user.passwordHash)) throw DomainException("invalid credentials")
        return AuthResult(user, createSession(user.id))
    }

    override suspend fun validateSession(token: String): UUID {
        val session = repo.findSession(token) ?: throw DomainException("unauthorized")
        if (clock.instant().isAfter(session.expiresAt)) {
            repo.deleteSession(token)
            throw DomainException("session expired")
        }
        return session.userId
    }

    suspend fun logout(token: String) = repo.deleteSession(token)

    override suspend fun findUserIdByEmail(email: String): UUID? = repo.findUserByEmail(email)?.id

    private suspend fun createSession(userId: UUID): Session {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val session = Session(
            token = HexFormat.of().formatHex(bytes),
            userId = userId,
            expiresAt = clock.instant().plus(Duration.ofDays(7)),
        )
        repo.saveSession(session)
        return session
    }
}
