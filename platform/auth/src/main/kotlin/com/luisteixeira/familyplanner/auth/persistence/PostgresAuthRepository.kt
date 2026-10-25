package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.Session
import com.luisteixeira.familyplanner.auth.model.User
import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import java.sql.SQLException
import java.time.ZoneOffset

class PostgresAuthRepository (
    private val database: Database
): AuthRepository {

    override suspend fun saveUser(user: User) {
        try {
            dbTransaction(database) {
                UsersTable.insert {
                    it[id] = user.id
                    it[email] = user.email
                    it[displayName] = user.displayName
                    it[createdAt] = user.createdAt.atOffset(ZoneOffset.UTC)
                    it[passwordHash] = user.passwordHash
                }
            }
        } catch (e: Exception) {
            if (e.isUniqueViolation()) {
                throw DuplicateEntryException("user already exists")
            }
            throw e
        }
    }

    override suspend fun findUserByEmail(email: String): User? {
        return dbTransaction(database) {
            UsersTable.selectAll().where {
                UsersTable.email eq email
            }.singleOrNull()?.toUser()
        }
    }

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

    private fun Exception.isUniqueViolation(): Boolean {
        return generateSequence<Throwable>(this) { it.cause }.any {
            it is SQLException && it.sqlState == "23505"
        }
    }

    private fun ResultRow.toUser(): User = User(
        id = this[UsersTable.id],
        email = this[UsersTable.email],
        displayName = this[UsersTable.displayName],
        createdAt = this[UsersTable.createdAt].toInstant(),
        passwordHash = this[UsersTable.passwordHash],
    )

    private fun ResultRow.toSession() = Session(
        token = this[SessionsTable.token],
        userId = this[SessionsTable.userId],
        expiresAt = this[SessionsTable.expiresAt].toInstant(),
    )

    private object UsersTable: Table("auth.users") {
        val id = javaUUID("id")
        val email = text("email")
        val displayName = text("display_name")
        val createdAt = timestampWithTimeZone("created_at")
        val passwordHash = text("password_hash")

        override val primaryKey = PrimaryKey(id)
    }

    private object SessionsTable : Table("auth.sessions") {
        val token = text("token")
        val userId = javaUUID("user_id")
        val expiresAt = timestampWithTimeZone("expires_at")

        override val primaryKey = PrimaryKey(token)
    }
}