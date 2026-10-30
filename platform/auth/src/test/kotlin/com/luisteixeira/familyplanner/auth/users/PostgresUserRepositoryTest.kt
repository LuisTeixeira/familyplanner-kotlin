package com.luisteixeira.familyplanner.auth.users

import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PostgresUserRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "auth", module = "auth")
            }
        }
    }

    private val repo by lazy { PostgresUserRepository(postgres.database) }

    @BeforeTest
    fun cleanTables() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE auth.sessions, auth.users CASCADE")
            }
        }
    }

    private fun user(email: String = "luis@mail.com") = User(
        id = UUID.randomUUID(),
        email = email,
        displayName = "Luis",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        passwordHash = "hash",
    )

    @Test
    fun savedUserCanBeFoundByEmail() = runBlocking<Unit> {
        val user = user()

        repo.saveUser(user)

        assertEquals(user, repo.findUserByEmail(user.email))
    }

    @Test
    fun unknownEmailReturnsNull() = runBlocking<Unit> {
        assertNull(repo.findUserByEmail("noone@mail.com"))
    }

    @Test
    fun duplicateEmailIsRejected() = runBlocking<Unit> {
        repo.saveUser(user("duplicate@mail.com"))

        assertFailsWith<DuplicateEntryException> {
            repo.saveUser(user("duplicate@mail.com"))
        }
    }
}
