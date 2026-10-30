package com.luisteixeira.familyplanner.auth.users

import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException
import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.sql.SQLException
import java.time.ZoneOffset

class PostgresUserRepository(
    private val database: Database,
) : UserRepository {

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

    private object UsersTable : Table("auth.users") {
        val id = javaUUID("id")
        val email = text("email")
        val displayName = text("display_name")
        val createdAt = timestampWithTimeZone("created_at")
        val passwordHash = text("password_hash")

        override val primaryKey = PrimaryKey(id)
    }
}
