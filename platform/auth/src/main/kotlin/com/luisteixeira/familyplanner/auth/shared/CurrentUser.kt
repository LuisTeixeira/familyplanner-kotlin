package com.luisteixeira.familyplanner.auth.shared

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import java.util.UUID

data class UserPrincipal(val userId: UUID)

fun ApplicationCall.currentUserIdOrNull(): UUID? = principal<UserPrincipal>()?.userId

fun ApplicationCall.currentUserId(): UUID =
    currentUserIdOrNull() ?: error("no authenticated user on this call")
