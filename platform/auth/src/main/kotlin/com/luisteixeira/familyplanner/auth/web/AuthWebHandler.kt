package com.luisteixeira.familyplanner.auth.web

import com.luisteixeira.familyplanner.auth.session.SESSION_COOKIE
import com.luisteixeira.familyplanner.auth.session.SessionService
import com.luisteixeira.familyplanner.auth.session.clearSessionCookie
import com.luisteixeira.familyplanner.auth.session.setSessionCookie
import com.luisteixeira.familyplanner.auth.users.UserService
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.core.web.hxRedirect
import com.luisteixeira.familyplanner.core.web.isHtmx
import com.luisteixeira.familyplanner.core.web.redirectSeeOther
import com.luisteixeira.familyplanner.core.web.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters

class AuthWebHandler(
    private val users: UserService,
    private val sessions: SessionService,
    private val templates: TemplateRegistry,
    private val secureCookies: Boolean,
) {

    suspend fun showLoginPage(call: ApplicationCall) =
        call.respondHtml(templates.render("login.html"))

    suspend fun showRegisterPage(call: ApplicationCall) =
        call.respondHtml(templates.render("register.html"))

    suspend fun register(call: ApplicationCall) {
        val form = call.receiveParameters()
        val email = form["email"].orEmpty()
        val displayName = form["display_name"].orEmpty()

        val user = try {
            users.register(email, form["password"].orEmpty(), displayName)
        } catch (e: DomainException) {
            call.respondHtml(
                templates.render(
                    "register.html",
                    mapOf("error" to e.message, "email" to email, "displayName" to displayName),
                ),
                HttpStatusCode.BadRequest,
            )
            return
        }

        call.setSessionCookie(sessions.create(user.id), secureCookies)
        if (call.isHtmx()) return call.hxRedirect("/app/households")
        call.redirectSeeOther("/app/households")
    }

    suspend fun login(call: ApplicationCall) {
        val form = call.receiveParameters()
        val email = form["email"].orEmpty()

        val user = try {
            users.authenticate(email, form["password"].orEmpty())
        } catch (e: DomainException) {
            call.respondHtml(
                templates.render(
                    "login.html",
                    mapOf("error" to "Invalid email or password", "email" to email),
                ),
                HttpStatusCode.Unauthorized,
            )
            return
        }

        call.setSessionCookie(sessions.create(user.id), secureCookies)
        if (call.isHtmx()) return call.hxRedirect("/app/households")
        call.redirectSeeOther("/app/households")
    }

    suspend fun logout(call: ApplicationCall) {
        call.request.cookies[SESSION_COOKIE]?.let { sessions.delete(it) }
        call.clearSessionCookie(secureCookies)

        if (call.isHtmx()) return call.hxRedirect("/app/login")
        call.redirectSeeOther("/app/login")
    }
}
