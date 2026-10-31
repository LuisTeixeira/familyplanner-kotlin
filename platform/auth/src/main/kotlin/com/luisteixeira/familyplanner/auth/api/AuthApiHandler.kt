package com.luisteixeira.familyplanner.auth.api

import com.luisteixeira.familyplanner.auth.token.TokenService
import com.luisteixeira.familyplanner.auth.users.LoginRequest
import com.luisteixeira.familyplanner.auth.users.RegisterRequest
import com.luisteixeira.familyplanner.auth.users.User
import com.luisteixeira.familyplanner.auth.users.UserService
import com.luisteixeira.familyplanner.auth.users.toDto
import com.luisteixeira.familyplanner.core.web.MessageResponse
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.receiveJsonOr400
import com.luisteixeira.familyplanner.core.web.respondJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

class AuthApiHandler(
    private val users: UserService,
    private val tokens: TokenService,
) {

    suspend fun register(call: ApplicationCall) {
        val request = call.receiveJsonOr400<RegisterRequest>("Invalid request payload")

        val user = httpCatch(HttpStatusCode.BadRequest) {
            users.register(request.email, request.password, request.displayName)
        }

        call.respondAuth(user)
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receiveJsonOr400<LoginRequest>("Invalid request payload")

        val user = httpCatch(HttpStatusCode.Unauthorized) { users.authenticate(request.email, request.password) }

        call.respondAuth(user)
    }

    suspend fun refresh(call: ApplicationCall) {
        val request = call.receiveJsonOr400<RefreshRequest>("Invalid request payload")

        val pair = httpCatch(HttpStatusCode.Unauthorized) {
            tokens.refresh(request.refreshToken)
        }

        call.respondJson(TokenResponse(pair.accessToken, pair.refreshToken, pair.accessTokenExpiresIn))
    }

    suspend fun logout(call: ApplicationCall) {
        val request = call.receiveJsonOr400<RefreshRequest>("Invalid request payload")

        tokens.logout(request.refreshToken)

        call.respondJson(MessageResponse("Logged out successfully"))
    }

    private suspend fun ApplicationCall.respondAuth(user: User) {
        val pair = tokens.issue(user.id)
        respondJson(AuthResponse(user.toDto(), pair.accessToken, pair.refreshToken, pair.accessTokenExpiresIn))
    }
}
