package com.luisteixeira.familyplanner.household.web

import com.luisteixeira.familyplanner.auth.currentUserId
import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.core.web.escapeHtml
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.hxRedirect
import com.luisteixeira.familyplanner.core.web.isHtmx
import com.luisteixeira.familyplanner.core.web.redirectSeeOther
import com.luisteixeira.familyplanner.core.web.respondHtml
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import com.luisteixeira.familyplanner.household.DashboardWidget
import com.luisteixeira.familyplanner.household.HouseholdService
import com.luisteixeira.familyplanner.household.householdId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters

class HouseholdWebHandler(
    private val service: HouseholdService,
    private val templates: TemplateRegistry,
    dashboardWidgets: List<DashboardWidget>,
) {
    private val widgets = dashboardWidgets.sortedBy { it.order }

    suspend fun listMyHouseholds(call: ApplicationCall) {
        val userId = call.currentUserId()

        call.respondHtml(
            templates.render(
                "households.html",
                mapOf(
                    "households" to service.listUserHouseholds(userId),
                    "invitations" to service.listPendingInvitations(userId),
                ),
            ),
        )
    }

    suspend fun create(call: ApplicationCall) {
        val userId = call.currentUserId()
        val name = call.receiveParameters()["name"].orEmpty()

        val household = httpCatch(HttpStatusCode.BadRequest) { service.createHousehold(userId, name) }

        if (call.isHtmx()) {
            call.respondHtml(
                templates.render("household_card.html", mapOf("household" to household)),
                HttpStatusCode.Created,
            )
            return
        }
        call.redirectSeeOther("/app/households")
    }

    suspend fun showDashboard(call: ApplicationCall) {
        val householdId = call.householdId()

        val household = httpCatch(HttpStatusCode.NotFound) { service.getHousehold(householdId) }
        val renderedWidgets = widgets.map { it.render(householdId) }

        call.respondHtml(
            templates.render(
                "household_dashboard.html",
                mapOf("household" to household, "widgets" to renderedWidgets),
            ),
        )
    }

    suspend fun inviteUser(call: ApplicationCall) {
        val householdId = call.householdId()
        val userId = call.currentUserId()
        val email = call.receiveParameters()["email"].orEmpty()

        if (email.isEmpty()) throw HttpError(HttpStatusCode.BadRequest, "Email is required")

        httpCatch(HttpStatusCode.BadRequest) { service.inviteUser(householdId, userId, email) }

        if (call.isHtmx()) {
            call.respondHtml("""<div class="success-msg">Invitation sent to ${email.escapeHtml()}!<div>""")
            return
        }
        call.redirectSeeOther("/app/households/$householdId")
    }

    suspend fun acceptInvitation(call: ApplicationCall) {
        val userId = call.currentUserId()
        val invitationId = call.parameters["invitationID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid invitation ID")

        httpCatch(HttpStatusCode.BadRequest) { service.acceptInvitation(invitationId, userId) }

        call.hxRedirect("/app/households")
    }

    suspend fun declineInvitation(call: ApplicationCall) {
        val userId = call.currentUserId()
        val invitationId = call.parameters["invitationID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid invitation ID")

        httpCatch(HttpStatusCode.BadRequest) { service.declineInvitation(invitationId, userId) }

        if (call.isHtmx()) return call.hxRedirect("/app/households")
        call.redirectSeeOther("/app/households")
    }
}
