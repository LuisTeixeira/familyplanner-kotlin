package com.luisteixeira.familyplanner.auth.token

import com.luisteixeira.familyplanner.auth.users.PostgresUserRepository
import com.luisteixeira.familyplanner.auth.users.User
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresDatabase
import com.luisteixeira.familyplanner.core.persistence.postgres.connectPostgres
import com.luisteixeira.familyplanner.core.persistence.postgres.migrate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgresRefreshTokenRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "auth", module = "auth")
            }
        }
    }

    private val repo by lazy { PostgresRefreshTokenRepository(postgres.database) }
    private val users by lazy { PostgresUserRepository(postgres.database) }
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val expiresAt = Instant.parse("2026-12-01T00:00:00Z")
    private val userId = UUID.randomUUID()

    @BeforeTest
    fun cleanTables() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE auth.refresh_tokens, auth.users CASCADE")
            }
        }
        runBlocking {
            users.saveUser(User(userId, "owner-$userId@example.com", "Owner", now, "hash"))
        }
    }

    private fun token(familyId: UUID, hash: String = "hash-${UUID.randomUUID()}", revokedAt: Instant? = null) =
        RefreshToken(
            tokenHash = hash,
            userId = userId,
            familyId = familyId,
            expiresAt = expiresAt,
            revokedAt = revokedAt,
        )

    @Test
    fun savedTokenCanBeFoundByHash() = runBlocking<Unit> {
        val token = token(UUID.randomUUID())

        repo.saveToken(token)

        assertEquals(token, repo.findByHash(token.tokenHash))
    }

    @Test
    fun revokeFamilyRevokesOnlyThatFamily() = runBlocking<Unit> {
        val family = UUID.randomUUID()
        val otherFamily = UUID.randomUUID()
        val mine = token(family)
        val theirs = token(otherFamily)
        repo.saveToken(mine)
        repo.saveToken(theirs)
        val revokedAt = Instant.parse("2026-06-01T00:00:00Z")

        repo.revokeFamily(family, revokedAt)

        assertEquals(revokedAt, repo.findByHash(mine.tokenHash)?.revokedAt)
        assertNull(repo.findByHash(theirs.tokenHash)?.revokedAt)
    }

    @Test
    fun revokeFamilyKeepsExistingRevocationTime() = runBlocking<Unit> {
        val family = UUID.randomUUID()
        val earlier = Instant.parse("2026-03-01T00:00:00Z")
        val rotated = token(family, revokedAt = earlier)
        repo.saveToken(rotated)

        repo.revokeFamily(family, Instant.parse("2026-06-01T00:00:00Z"))

        assertEquals(earlier, repo.findByHash(rotated.tokenHash)?.revokedAt)
    }

    @Test
    fun revokeIfActiveClaimsATokenOnlyOnce() = runBlocking {
        val token = token(UUID.randomUUID())
        repo.saveToken(token)

        assertTrue(repo.revokeIfActive(token.tokenHash, Instant.parse("2026-06-01T00:00:00Z")))
        assertFalse(repo.revokeIfActive(token.tokenHash, Instant.parse("2026-06-02T00:00:00Z")))
    }

    @Test
    fun revokeIfActiveReturnsFalseForUnknownToken() = runBlocking {
        assertFalse(repo.revokeIfActive("missing", Instant.parse("2026-06-01T00:00:00Z")))
    }
}
