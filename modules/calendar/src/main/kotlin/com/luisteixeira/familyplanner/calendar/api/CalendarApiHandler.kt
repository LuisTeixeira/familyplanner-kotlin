package com.luisteixeira.familyplanner.calendar.api

import com.luisteixeira.familyplanner.auth.currentUserId
import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.calendar.dto.EventRequest
import com.luisteixeira.familyplanner.calendar.dto.eventRange
import com.luisteixeira.familyplanner.calendar.dto.toDto
import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.MessageResponse
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.receiveJsonOr400
import com.luisteixeira.familyplanner.core.web.respondJson
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import com.luisteixeira.familyplanner.household.householdId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import java.time.ZoneId

class CalendarApiHandler(
    private val service: CalendarService,
    private val defaultZone: ZoneId,
) {

    suspend fun createEvent(call: ApplicationCall) {
        val householdId = call.householdId()
        val userId = call.currentUserId()
        val request = call.receiveJsonOr400<EventRequest>("Invalid input payload")

        val event = httpCatch(HttpStatusCode.BadRequest) {
            service.createEvent(householdId, userId, request.toInput(defaultZone))
        }

        call.respondJson(event.toDto(), HttpStatusCode.Created)
    }

    suspend fun listEvents(call: ApplicationCall) {
        val householdId = call.householdId()
        val params = call.request.queryParameters

        val range = httpCatch(HttpStatusCode.BadRequest) {
            eventRange(params["from"], params["to"], params["timezone"], defaultZone)
        }
        val events = httpCatch(HttpStatusCode.BadRequest) {
            service.listEvents(householdId, range.from, range.to)
        }

        call.respondJson(events.map { it.toDto() })
    }

    suspend fun updateEvent(call: ApplicationCall) {
        val householdId = call.householdId()
        val eventId = call.parameters["eventID"].toUuidOrNull() ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid event ID")
        val request = call.receiveJsonOr400<EventRequest>("Invalid input payload")

        val event = httpCatch(HttpStatusCode.BadRequest) {
            service.updateEvent(householdId, eventId, request.toInput(defaultZone))
        }

        call.respondJson(event.toDto())
    }

    suspend fun deleteEvent(call: ApplicationCall) {
        val householdId = call.householdId()
        val eventId = call.parameters["eventID"].toUuidOrNull() ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid event ID")

        httpCatch(HttpStatusCode.NotFound) {
            service.deleteEvent(householdId, eventId)
        }

        call.respondJson(MessageResponse("Event deleted successfully"))
    }
}