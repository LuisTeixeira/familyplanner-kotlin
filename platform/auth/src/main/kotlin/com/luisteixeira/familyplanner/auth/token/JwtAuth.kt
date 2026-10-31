package com.luisteixeira.familyplanner.auth.token

import com.luisteixeira.familyplanner.auth.shared.UserPrincipal
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationFailedCause
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.response.respondText

const val JWT_AUTH = "jwt"

class JwtAuthConfig(name: String, val verifier: TokenVerifier) : AuthenticationProvider.Config(name)

class JwtAuthProvider(config: JwtAuthConfig) : AuthenticationProvider(config) {
    private val verifier = config.verifier

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val token = call.request.headers[HttpHeaders.Authorization]
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.ifEmpty { null }
        val userId = token?.let { verifier.verify(it) }

        if (userId == null) {
            context.challenge(JWT_AUTH, AuthenticationFailedCause.InvalidCredentials) { challenge, c ->
                c.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                challenge.complete()
            }
            return
        }

        context.principal(UserPrincipal(userId))
    }
}

fun AuthenticationConfig.jwtAuth(verifier: TokenVerifier) {
    register(JwtAuthProvider(JwtAuthConfig(JWT_AUTH, verifier)))
}