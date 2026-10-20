package com.luisteixeira.familyplanner.shopping

import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.household.HouseholdAccess
import com.luisteixeira.familyplanner.household.householdScoped
import com.luisteixeira.familyplanner.shopping.api.ShoppingApiHandler
import com.luisteixeira.familyplanner.shopping.web.ShoppingWebHandler
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class ShoppingModule(
    private val householdAccess: HouseholdAccess,
    private val webHandler: ShoppingWebHandler,
    private val apiHandler: ShoppingApiHandler,
) : AppModule {

    companion object {
        const val NAME = "shopping"
    }

    override val name = NAME

    override fun registerProtectedWebRoutes(r: Route) {
        r.route("/households/{householdID}/shopping") {
            householdScoped(householdAccess)
            get("/lists") { webHandler.listDashboard(call) }
            post("/lists") { webHandler.createList(call) }
            get("/lists/{listID}") { webHandler.getList(call) }
            post("/lists/{listID}/items") { webHandler.addItem(call) }
            patch("/items/{itemID}/toggle") { webHandler.toggleItem(call) }
        }
    }

    override fun registerProtectedApiRoutes(r: Route) {
        r.route("/households/{householdID}/shopping") {
            householdScoped(householdAccess)
            post("/lists") { apiHandler.createList(call) }
            get("/lists/{listID}") { apiHandler.getList(call) }
            post("/lists/{listID}/items") { apiHandler.addItem(call) }
            patch("/items/{itemID}/toggle") { apiHandler.toggleItem(call) }
        }
    }
}
