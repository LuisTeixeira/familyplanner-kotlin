package com.luisteixeira.familyplanner.household.api

import com.luisteixeira.familyplanner.auth.shared.currentUserId
import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.MessageResponse
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.receiveJsonOr400
import com.luisteixeira.familyplanner.core.web.respondJson
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import com.luisteixeira.familyplanner.household.HouseholdService
import com.luisteixeira.familyplanner.household.dto.CreateHouseholdRequest
import com.luisteixeira.familyplanner.household.dto.HouseholdDto
import com.luisteixeira.familyplanner.household.dto.InviteRequest
import com.luisteixeira.familyplanner.household.dto.InvitationDto
import com.luisteixeira.familyplanner.household.dto.toDto
import com.luisteixeira.familyplanner.household.householdId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

class HouseholdApiHandler(private val service: HouseholdService) {

    suspend fun create(call: ApplicationCall) {
        val userId = call.currentUserId()
        val request = call.receiveJsonOr400<CreateHouseholdRequest>("Invalid input payload")

        val household = httpCatch(HttpStatusCode.BadRequest) { service.createHousehold(userId, request.name) }

        call.respondJson(household.toDto())
    }

    suspend fun listMyHouseholds(call: ApplicationCall) {
        val userId = call.currentUserId()

        call.respondJson(service.listUserHouseholds(userId).map { it.toDto() })
    }

    suspend fun inviteUser(call: ApplicationCall) {
        val householdId = call.householdId()
        val userId = call.currentUserId()
        val request = call.receiveJsonOr400<InviteRequest>("Invalid input payload")

        if (request.email.isEmpty()) throw HttpError(HttpStatusCode.BadRequest, "Email is required")

        val invitation = httpCatch(HttpStatusCode.BadRequest) {
            service.inviteUser(householdId, userId, request.email)
        }

        call.respondJson(invitation.toDto(), HttpStatusCode.Created)
    }

    suspend fun acceptInvitation(call: ApplicationCall) {
        val userId = call.currentUserId()
        val invitationId = call.parameters["invitationID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid invitation ID")

        httpCatch(HttpStatusCode.BadRequest) { service.acceptInvitation(invitationId, userId) }

        call.respondJson(MessageResponse("Invitation accepted successfully"))
    }

    suspend fun declineInvitation(call: ApplicationCall) {
        val userId = call.currentUserId()
        val invitationId = call.parameters["invitationID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid invitation ID")

        httpCatch(HttpStatusCode.BadRequest) { service.declineInvitation(invitationId, userId) }

        call.respondJson(MessageResponse("Invitation declined successfully"))
    }
}
