package com.luisteixeira.familyplanner.auth.session

import com.luisteixeira.familyplanner.auth.users.PostgresUserRepository
import com.luisteixeira.familyplanner.auth.users.User
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresDatabase
import com.luisteixeira.familyplanner.core.persistence.postgres.connectPostgres
import com.luisteixeira.familyplanner.core.persistence.postgres.migrate
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgresSessionRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "auth", module = "auth")
            }
        }
    }

    private val repo by lazy { PostgresSessionRepository(postgres.database) }
    private val users by lazy { PostgresUserRepository(postgres.database) }

    @BeforeTest
    fun cleanTables() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE auth.sessions, auth.users CASCADE")
            }
        }
    }

    @Test
    fun savedSessionCanBeFoundAndDeleted() = runBlocking<Unit> {
        val owner = User(
            id = UUID.randomUUID(),
            email = "luis@mail.com",
            displayName = "Luis",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            passwordHash = "hash",
        )
        users.saveUser(owner)
        val session = Session(
            token = "token-123",
            userId = owner.id,
            expiresAt = Instant.parse("2026-01-08T00:00:00Z"),
        )

        repo.saveSession(session)
        assertEquals(session, repo.findSession(session.token))

        repo.deleteSession(session.token)
        assertNull(repo.findSession(session.token))
    }
}
