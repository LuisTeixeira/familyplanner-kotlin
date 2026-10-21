package com.luisteixeira.familyplanner.calendar.api

import com.luisteixeira.familyplanner.auth.currentUserId
import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.calendar.dto.CreateEventRequest
import com.luisteixeira.familyplanner.calendar.dto.eventRange
import com.luisteixeira.familyplanner.calendar.dto.toDto
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.receiveJsonOr400
import com.luisteixeira.familyplanner.core.web.respondJson
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
        val request = call.receiveJsonOr400<CreateEventRequest>("Invalid input payload")

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
}