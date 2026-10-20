package com.luisteixeira.familyplanner.core.module

import io.ktor.server.routing.Route

interface AppModule {
    val name: String

    fun registerPublicWebRoutes(r: Route) {}
    fun registerProtectedWebRoutes(r: Route) {}
    fun registerPublicApiRoutes(r: Route) {}
    fun registerProtectedApiRoutes(r: Route) {}
}
