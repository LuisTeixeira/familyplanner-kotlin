package com.luisteixeira.familyplanner.auth.apikey

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

class PostgresApiClientRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "auth", module = "auth")
            }
        }
    }

    private val repo by lazy { PostgresApiClientRepository(postgres.database) }
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeTest
    fun cleanTable() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE auth.api_clients")
            }
        }
    }

    private fun client(keyHash: String = "hash-${UUID.randomUUID()}") = ApiClient(
        id = UUID.randomUUID(),
        name = "Mobile",
        keyHash = keyHash,
        createdAt = createdAt,
        revokedAt = null,
    )

    @Test
    fun savedClientCanBeFoundByKeyHash() = runBlocking {
        val client = client()
        repo.saveClient(client)
        assertEquals(client, repo.findByKeyHash(client.keyHash))
    }

    @Test
    fun unknownKeyHashReturnsNull() = runBlocking {
        assertNull(repo.findByKeyHash("missing"))
    }

    @Test
    fun revokeClientKeepsItsRevocationTime() = runBlocking {
        val client = client()
        repo.saveClient(client)
        val revokedAt = Instant.parse("2026-02-01T00:00:00Z")

        repo.saveClient(client.copy(revokedAt = revokedAt))

        assertEquals(revokedAt, repo.findByKeyHash(client.keyHash)?.revokedAt)
    }

}
