package com.luisteixeira.familyplanner.calendar.web

import com.luisteixeira.familyplanner.auth.shared.currentUserId
import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.calendar.model.Event
import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.utils.parseDate
import com.luisteixeira.familyplanner.calendar.utils.parseTime
import com.luisteixeira.familyplanner.calendar.utils.parseZone
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.core.web.escapeHtml
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.hxRedirect
import com.luisteixeira.familyplanner.core.web.isHtmx
import com.luisteixeira.familyplanner.core.web.redirectSeeOther
import com.luisteixeira.familyplanner.core.web.respondHtml
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import com.luisteixeira.familyplanner.household.householdId
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class CalendarWebHandler(
    private val service: CalendarService,
    private val templates: TemplateRegistry,
    private val defaultTimezone: ZoneId,
) {

    suspend fun showWeek(call: ApplicationCall) {
        val householdId = call.householdId()
        val params = call.request.queryParameters

        val (zone, day) = httpCatch(HttpStatusCode.BadRequest) {
            val zone = params["timezone"]?.let { parseZone(it) } ?: defaultTimezone
            val day = params["week"]?.let { parseDate(it) } ?: LocalDate.now(zone)
            zone to day
        }
        val start = weekStart(day)
        val events = httpCatch(HttpStatusCode.BadRequest) {
            service.listEvents(
                householdId,
                start.atStartOfDay(zone).toInstant(),
                start.plusWeeks(1).atStartOfDay(zone).toInstant(),
            )
        }

        call.respondHtml(
            templates.render(
                "calendar_week.html",
                mapOf(
                    "householdId" to householdId,
                    "week" to weekView(start, zone, events),
                    "timezone" to zone.id,
                    "today" to LocalDate.now(zone),
                ),
            ),
        )
    }

    suspend fun createEvent(call: ApplicationCall) {
        val householdId = call.householdId()
        val userId = call.currentUserId()
        val form = call.receiveParameters()

        val event = try {
            service.createEvent(householdId, userId, form.toEventInput())
        } catch (e: DomainException) {
            respondFormError(call, e)
            return
        }

        respondWithWeekOf(call, householdId, event)
    }

    suspend fun newEventForm(call: ApplicationCall) {
        val householdId = call.householdId()
        val timezone = httpCatch(HttpStatusCode.BadRequest) {
            call.request.queryParameters["timezone"]?.let { parseZone(it) } ?: defaultTimezone
        }
        call.respondHtml(templates.render("calendar_event_modal.html", modalModel(householdId, null, timezone)))
    }

    suspend fun editEventForm(call: ApplicationCall) {
        val householdId = call.householdId()
        val eventId = eventId(call)

        val event = httpCatch(HttpStatusCode.NotFound) {
            service.getEvent(householdId, eventId)
        }

        call.respondHtml(templates.render("calendar_event_modal.html", modalModel(householdId, event, event.timezone)))
    }

    suspend fun updateEvent(call: ApplicationCall) {
        val householdId = call.householdId()
        val eventId = eventId(call)
        val form = call.receiveParameters()

        val event = try {
            service.updateEvent(householdId, eventId, form.toEventInput())
        } catch (e: DomainException) {
            respondFormError(call, e)
            return
        }

        respondWithWeekOf(call, householdId, event)
    }

    suspend fun deleteEvent(call: ApplicationCall) {
        val householdId = call.householdId()
        val eventId = eventId(call)

        val event = httpCatch(HttpStatusCode.NotFound) {
            service.getEvent(householdId, eventId)
        }
        httpCatch(HttpStatusCode.NotFound) {
            service.deleteEvent(householdId, eventId)
        }

        respondWithWeekOf(call, householdId, event)
    }

    private fun eventId(call: ApplicationCall): UUID {
        return call.parameters["eventID"].toUuidOrNull() ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid event ID")
    }

    private fun modalModel(householdId: UUID, event: Event?, timezone: ZoneId): Map<String, Any?> = mapOf(
        "householdId" to householdId,
        "event" to event,
        "timezone" to timezone,
        "today" to LocalDate.now(timezone)
    )

    private fun Parameters.toEventInput(): EventInput = EventInput(
        title = this["title"].orEmpty(),
        date = parseDate(this["date"].orEmpty()),
        time = this["time"]?.takeIf { it.isNotBlank() }?.let { parseTime(it) },
        timezone = this["timezone"]?.takeIf { it.isNotBlank() }?.let { parseZone(it) } ?: defaultTimezone,
        description = this["description"],
        address = this["address"],
    )

    private suspend fun respondFormError(call: ApplicationCall, error: DomainException) {
        call.respondHtml(
            """<div class="error">${error.message.orEmpty().escapeHtml()}</div>""",
            HttpStatusCode.UnprocessableEntity,
        )
    }

    private suspend fun respondWithWeekOf(call: ApplicationCall, householdId: UUID, event: Event) {
        val weekUrl = "/app/households/$householdId/calendar" + "?week=${weekStart(event.date)}&timezone=${event.timezone.id}"

        if (call.isHtmx()) {
            return call.hxRedirect(weekUrl)
        }
        call.redirectSeeOther(weekUrl)
    }
}
