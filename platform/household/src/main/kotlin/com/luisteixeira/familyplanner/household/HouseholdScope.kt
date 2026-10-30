package com.luisteixeira.familyplanner.household

import com.luisteixeira.familyplanner.auth.shared.currentUserIdOrNull
import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.AuthenticationChecked
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import java.util.UUID

private val HouseholdIdKey = AttributeKey<UUID>("householdID")

class HouseholdScopeConfig {
    lateinit var access: HouseholdAccess
}

private val HouseholdScope = createRouteScopedPlugin("HouseholdScope", ::HouseholdScopeConfig) {
    val access = pluginConfig.access

    on(AuthenticationChecked) { call ->
        val userId = call.currentUserIdOrNull() ?: throw HttpError(HttpStatusCode.Unauthorized, "Unauthorized")
        val householdId = call.parameters["householdID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid Household ID")

        if (!access.isMember(householdId, userId)) {
            throw HttpError(HttpStatusCode.Forbidden, "Forbidden: Access denied to this household")
        }
        call.attributes.put(HouseholdIdKey, householdId)
    }
}

fun Route.householdScoped(access: HouseholdAccess) {
    install(HouseholdScope) { this.access = access }
}

fun ApplicationCall.householdId(): UUID =
    attributes.getOrNull(HouseholdIdKey) ?: error("no household on this call")
