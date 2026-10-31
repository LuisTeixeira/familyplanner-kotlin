package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.apikey.ApiKeyGuard
import com.luisteixeira.familyplanner.auth.session.SESSION_AUTH
import com.luisteixeira.familyplanner.auth.csrf.CsrfProtection
import com.luisteixeira.familyplanner.auth.session.sessionAuth
import com.luisteixeira.familyplanner.auth.token.JWT_AUTH
import com.luisteixeira.familyplanner.auth.token.jwtAuth
import com.luisteixeira.familyplanner.core.module.PLATFORM_MODULE
import com.luisteixeira.familyplanner.core.module.staticRoot
import com.luisteixeira.familyplanner.core.web.HttpError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("familyplanner")

fun Application.familyPlannerApp(app: ComposedApp) {
    monitor.subscribe(ApplicationStopped) { app.close() }
    install(CallLogging)

    install(Authentication) {
        sessionAuth(app.sessionValidator)
        jwtAuth(app.tokenVerifier)
    }

    install(StatusPages) {
        exception<HttpError> { call, cause ->
            call.respondText(cause.message ?: cause.status.description, status = cause.status)
        }
        exception<Throwable> { call, cause ->
            log.error("unhandled error on ${call.request.local.uri}", cause)
            call.respondText("Internal Server Error", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        (listOf(PLATFORM_MODULE) + app.modules.map { it.name }).forEach { module ->
            staticResources("/static/$module", staticRoot(module))
        }

        route("/app") {
            install(CsrfProtection) {
                tokens = app.csrfTokens
                secureCookies = app.secureCookies
            }

            app.modules.forEach { it.registerPublicWebRoutes(this) }

            authenticate(SESSION_AUTH) {
                app.modules.forEach { it.registerProtectedWebRoutes(this) }
            }
        }

        route("/api/v1") {
            install(ApiKeyGuard) {
                service = app.apiKeys
            }

            app.modules.forEach { it.registerPublicApiRoutes(this) }

            authenticate(JWT_AUTH) {
                app.modules.forEach { it.registerProtectedApiRoutes(this) }
            }
        }
    }
}
