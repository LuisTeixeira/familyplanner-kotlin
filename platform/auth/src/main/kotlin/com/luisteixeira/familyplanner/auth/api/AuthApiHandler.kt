package com.luisteixeira.familyplanner.auth.api

import com.luisteixeira.familyplanner.auth.session.SESSION_COOKIE
import com.luisteixeira.familyplanner.auth.session.SessionService
import com.luisteixeira.familyplanner.auth.session.clearSessionCookie
import com.luisteixeira.familyplanner.auth.session.setSessionCookie
import com.luisteixeira.familyplanner.auth.users.LoginRequest
import com.luisteixeira.familyplanner.auth.users.RegisterRequest
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
    private val sessions: SessionService,
    private val secureCookies: Boolean,
) {

    suspend fun register(call: ApplicationCall) {
        val request = call.receiveJsonOr400<RegisterRequest>("Invalid request payload")

        val user = httpCatch(HttpStatusCode.BadRequest) {
            users.register(request.email, request.password, request.displayName)
        }

        val session = sessions.create(user.id)
        call.setSessionCookie(session, secureCookies)
        call.respondJson(AuthResponse(user.toDto(), session.token))
    }

    suspend fun login(call: ApplicationCall) {
        val request = call.receiveJsonOr400<LoginRequest>("Invalid request payload")

        val user = httpCatch(HttpStatusCode.Unauthorized) { users.authenticate(request.email, request.password) }

        val session = sessions.create(user.id)
        call.setSessionCookie(session, secureCookies)
        call.respondJson(AuthResponse(user.toDto(), session.token))
    }

    suspend fun logout(call: ApplicationCall) {
        call.request.cookies[SESSION_COOKIE]?.let { sessions.delete(it) }
        call.clearSessionCookie(secureCookies)

        call.respondJson(MessageResponse("Logged out successfully"))
    }
}
