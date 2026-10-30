package com.luisteixeira.familyplanner.auth.session

import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.ZoneOffset

class PostgresSessionRepository(
    private val database: Database,
) : SessionRepository {

    override suspend fun saveSession(session: Session) {
        dbTransaction(database) {
            SessionsTable.insert {
                it[token] = session.token
                it[userId] = session.userId
                it[expiresAt] = session.expiresAt.atOffset(ZoneOffset.UTC)
            }
        }
    }

    override suspend fun findSession(token: String): Session? {
        return dbTransaction(database) {
            SessionsTable.selectAll().where {
                SessionsTable.token eq token
            }.singleOrNull()?.toSession()
        }
    }

    override suspend fun deleteSession(token: String) {
        dbTransaction(database) {
            SessionsTable.deleteWhere { SessionsTable.token eq token }
        }
    }

    private fun ResultRow.toSession() = Session(
        token = this[SessionsTable.token],
        userId = this[SessionsTable.userId],
        expiresAt = this[SessionsTable.expiresAt].toInstant(),
    )

    private object SessionsTable : Table("auth.sessions") {
        val token = text("token")
        val userId = javaUUID("user_id")
        val expiresAt = timestampWithTimeZone("expires_at")

        override val primaryKey = PrimaryKey(token)
    }
}
