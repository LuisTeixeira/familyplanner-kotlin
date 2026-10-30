package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.RefreshToken
import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class PostgresRefreshTokenRepository(
    val database: Database
) : RefreshTokenRepository {

    override suspend fun saveToken(token: RefreshToken) {
        dbTransaction(database) {
            val updated = RefreshTokensTable.update({ RefreshTokensTable.tokenHash eq token.tokenHash }) {
                it[revokedAt] = token.revokedAt?.atOffset(ZoneOffset.UTC)
            }
            if (updated == 0) {
                RefreshTokensTable.insert {
                    it[tokenHash] = token.tokenHash
                    it[userId] = token.userId
                    it[familyId] = token.familyId
                    it[expiresAt] = token.expiresAt.atOffset(ZoneOffset.UTC)
                    it[revokedAt] = token.revokedAt?.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findByHash(tokenHash: String): RefreshToken? {
        return dbTransaction(database) {
            RefreshTokensTable.selectAll().where{
                RefreshTokensTable.tokenHash eq tokenHash
            }.singleOrNull()?.toRefreshToken()
        }
    }

    override suspend fun revokeIfActive(tokenHash: String, at: Instant): Boolean {
        return dbTransaction(database) {
            val updated = RefreshTokensTable.update({
                (RefreshTokensTable.tokenHash eq tokenHash) and RefreshTokensTable.revokedAt.isNull()
            }) {
                it[revokedAt] = at.atOffset(ZoneOffset.UTC)
            }
            updated > 0
        }
    }

    override suspend fun revokeFamily(familyId: UUID, at: Instant) {
        dbTransaction(database) {
            RefreshTokensTable.update({
                (RefreshTokensTable.familyId eq familyId) and RefreshTokensTable.revokedAt.isNull()
            }) {
                it[revokedAt] = at.atOffset(ZoneOffset.UTC)
            }
        }
    }

    private fun ResultRow.toRefreshToken(): RefreshToken = RefreshToken(
        tokenHash = this[RefreshTokensTable.tokenHash],
        userId = this[RefreshTokensTable.userId],
        familyId = this[RefreshTokensTable.familyId],
        expiresAt = this[RefreshTokensTable.expiresAt].toInstant(),
        revokedAt = this[RefreshTokensTable.revokedAt]?.toInstant(),
    )

    private object RefreshTokensTable : Table("auth.refresh_tokens") {
        val tokenHash = text("token_hash")
        val userId = javaUUID("user_id")
        val familyId = javaUUID("family_id")
        val expiresAt = timestampWithTimeZone("expires_at")
        val revokedAt = timestampWithTimeZone("revoked_at").nullable()

        override val primaryKey = PrimaryKey(tokenHash)
    }

}