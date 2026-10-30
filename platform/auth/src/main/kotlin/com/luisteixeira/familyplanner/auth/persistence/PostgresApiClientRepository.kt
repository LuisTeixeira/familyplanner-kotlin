package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.ApiClient
import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.ZoneOffset

class PostgresApiClientRepository(
    private val database: Database
) : ApiClientRepository{

    override suspend fun saveClient(client: ApiClient) {
        dbTransaction(database) {
            val updated = ApiClientsTable.update({ ApiClientsTable.id eq client.id }) {
                it[name] = client.name
                it[keyHash] = client.keyHash
                it[revokedAt] = client.revokedAt?.atOffset(ZoneOffset.UTC)
            }
            if (updated == 0) {
                ApiClientsTable.insert {
                    it[id] = client.id
                    it[name] = client.name
                    it[keyHash] = client.keyHash
                    it[createdAt] = client.createdAt.atOffset(ZoneOffset.UTC)
                    it[revokedAt] = client.revokedAt?.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findByKeyHash(keyHash: String): ApiClient? {
        return dbTransaction(database) {
            ApiClientsTable.selectAll().where {
                ApiClientsTable.keyHash eq keyHash
            }.singleOrNull()?.toApiClient()
        }
    }

    private fun ResultRow.toApiClient(): ApiClient = ApiClient(
        id = this[ApiClientsTable.id],
        name = this[ApiClientsTable.name],
        keyHash = this[ApiClientsTable.keyHash],
        createdAt = this[ApiClientsTable.createdAt].toInstant(),
        revokedAt = this[ApiClientsTable.revokedAt]?.toInstant(),
    )

    private object ApiClientsTable : Table("auth.api_clients") {
        val id = javaUUID("id")
        val name = text("name")
        val keyHash = text("key_hash")
        val createdAt = timestampWithTimeZone("created_at")
        val revokedAt = timestampWithTimeZone("revoked_at").nullable()

        override val primaryKey = PrimaryKey(id)
    }
}