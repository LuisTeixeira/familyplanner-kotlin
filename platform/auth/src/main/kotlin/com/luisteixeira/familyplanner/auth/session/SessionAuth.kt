package com.luisteixeira.familyplanner.auth.session

import com.luisteixeira.familyplanner.auth.shared.UserPrincipal
import com.luisteixeira.familyplanner.core.domain.DomainException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.response.respondText

const val SESSION_AUTH = "session"
const val SESSION_COOKIE = "session_token"

class SessionAuthConfig(name: String, val validator: SessionValidator) : AuthenticationProvider.Config(name)

class SessionAuthProvider(config: SessionAuthConfig) : AuthenticationProvider(config) {
    private val validator = config.validator

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val bearer = call.request.headers[HttpHeaders.Authorization]
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.ifEmpty { null }
        val token = bearer ?: call.request.cookies[SESSION_COOKIE]

        val userId = try {
            if (token.isNullOrEmpty()) null else validator.validateSession(token)
        } catch (e: DomainException) {
            null
        }

        if (userId == null) {
            context.challenge(SESSION_AUTH, AuthenticationFailedCause.NoCredentials) { challenge, c ->
                c.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                challenge.complete()
            }
            return
        }

        context.principal(UserPrincipal(userId))
    }
}

fun AuthenticationConfig.sessionAuth(validator: SessionValidator) {
    register(SessionAuthProvider(SessionAuthConfig(SESSION_AUTH, validator)))
}
