package com.luisteixeira.familyplanner.auth.api

import com.luisteixeira.familyplanner.auth.AuthService
import com.luisteixeira.familyplanner.auth.SESSION_COOKIE
import com.luisteixeira.familyplanner.auth.clearSessionCookie
import com.luisteixeira.familyplanner.auth.dto.AuthResponse
import com.luisteixeira.familyplanner.auth.dto.LoginRequest
import com.luisteixeira.familyplanner.auth.dto.RegisterRequest
import com.luisteixeira.familyplanner.auth.dto.toDto
import com.luisteixeira.familyplanner.auth.setSessionCookie
import com.luisteixeira.familyplanner.core.web.MessageResponse
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.receiveJsonOr400
import com.luisteixeira.familyplanner.core.web.respondJson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

class AuthApiHandler(private val service: AuthService) {

    suspend fun register(call: ApplicationCall) {
        val request = call.receiveJsonOr400<RegisterRequest>("Invalid request payload")

        val result = httpCatch(HttpStatusCode.BadRequest) {
            service.register(request.email, request.password, request.displayName)
        }

        call.setSessionCookie(result.session)
        call.respondJson(AuthResponse(result.user.toDto(), result.session.token))
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receiveJsonOr400<LoginRequest>("Invalid request payload")

        val result = httpCatch(HttpStatusCode.Unauthorized) { service.login(request.email, request.password) }

        call.setSessionCookie(result.session)
        call.respondJson(AuthResponse(result.user.toDto(), result.session.token))
    }

    suspend fun logout(call: ApplicationCall) {
        call.request.cookies[SESSION_COOKIE]?.let { service.logout(it) }
        call.clearSessionCookie()

        call.respondJson(MessageResponse("Logged out successfully"))
    }
}
