package com.luisteixeira.familyplanner.auth.users

import com.luisteixeira.familyplanner.auth.shared.UserDirectory
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.time.Clock
import java.util.UUID

class UserService(
    private val repo: UserRepository,
    private val clock: Clock = Clock.systemUTC(),
) : UserDirectory {

    suspend fun register(email: String, password: String, displayName: String): User {
        if (email.isEmpty() || password.isEmpty()) throw DomainException("email and password are required")

        val user = User(
            id = UUID.randomUUID(),
            email = email,
            displayName = displayName,
            createdAt = clock.instant(),
            passwordHash = withContext(Dispatchers.Default) { BCrypt.hashpw(password, BCrypt.gensalt()) },
        )
        try {
            repo.saveUser(user)
        } catch (e: DuplicateEntryException) {
            throw DomainException("user already exists")
        }
        return user
    }

    suspend fun authenticate(email: String, password: String): User {
        val user = repo.findUserByEmail(email) ?: throw DomainException("invalid credentials")
        val passwordMatches = withContext(Dispatchers.Default) { BCrypt.checkpw(password, user.passwordHash) }
        if (!passwordMatches) throw DomainException("invalid credentials")
        return user
    }

    override suspend fun findUserIdByEmail(email: String): UUID? = repo.findUserByEmail(email)?.id
}
