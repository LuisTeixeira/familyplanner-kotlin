package com.luisteixeira.familyplanner.household

import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.household.api.HouseholdApiHandler
import com.luisteixeira.familyplanner.household.web.HouseholdWebHandler
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

class HouseholdModule(
    private val service: HouseholdService,
    private val webHandler: HouseholdWebHandler,
    private val apiHandler: HouseholdApiHandler,
) : AppModule {

    companion object {
        const val NAME = "household"
    }

    override val name = NAME

    override fun registerProtectedWebRoutes(r: Route) {
        r.post("/invitations/{invitationID}/accept") { webHandler.acceptInvitation(call) }
        r.post("/invitations/{invitationID}/decline") { webHandler.declineInvitation(call) }

        r.get("/households") { webHandler.listMyHouseholds(call) }
        r.post("/households") { webHandler.create(call) }

        r.route("/households/{householdID}") {
            householdScoped(service)
            get { webHandler.showDashboard(call) }
            post("/invites") { webHandler.inviteUser(call) }
        }
    }

    override fun registerProtectedApiRoutes(r: Route) {
        r.post("/invitations/{invitationID}/accept") { apiHandler.acceptInvitation(call) }
        r.post("/invitations/{invitationID}/decline") { apiHandler.declineInvitation(call) }

        r.get("/households") { apiHandler.listMyHouseholds(call) }
        r.post("/households") { apiHandler.create(call) }

        r.route("/households/{householdID}") {
            householdScoped(service)
            post("/invites") { apiHandler.inviteUser(call) }
        }
    }
}
