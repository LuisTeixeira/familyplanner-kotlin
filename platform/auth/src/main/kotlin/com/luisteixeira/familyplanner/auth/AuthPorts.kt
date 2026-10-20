package com.luisteixeira.familyplanner.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import java.util.UUID

interface SessionValidator {
    suspend fun validateSession(token: String): UUID
}

interface UserDirectory {
    suspend fun findUserIdByEmail(email: String): UUID?
}

data class SessionPrincipal(val userId: UUID)

fun ApplicationCall.currentUserIdOrNull(): UUID? = principal<SessionPrincipal>()?.userId

fun ApplicationCall.currentUserId(): UUID =
    currentUserIdOrNull() ?: error("no authenticated user on this call")
