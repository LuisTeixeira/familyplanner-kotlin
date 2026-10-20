package com.luisteixeira.familyplanner.auth

import com.luisteixeira.familyplanner.auth.api.AuthApiHandler
import com.luisteixeira.familyplanner.auth.web.AuthWebHandler
import com.luisteixeira.familyplanner.core.module.AppModule
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

class AuthModule(
    private val webHandler: AuthWebHandler,
    private val apiHandler: AuthApiHandler,
) : AppModule {

    companion object {
        const val NAME = "auth"
    }

    override val name = NAME

    override fun registerPublicWebRoutes(r: Route) {
        r.get("/login") { webHandler.showLoginPage(call) }
        r.post("/login") { webHandler.login(call) }
        r.get("/register") { webHandler.showRegisterPage(call) }
        r.post("/register") { webHandler.register(call) }
    }

    override fun registerProtectedWebRoutes(r: Route) {
        r.post("/logout") { webHandler.logout(call) }
    }

    override fun registerPublicApiRoutes(r: Route) {
        r.post("/register") { apiHandler.register(call) }
        r.post("/login") { apiHandler.login(call) }
    }

    override fun registerProtectedApiRoutes(r: Route) {
        r.post("/logout") { apiHandler.logout(call) }
    }
}
