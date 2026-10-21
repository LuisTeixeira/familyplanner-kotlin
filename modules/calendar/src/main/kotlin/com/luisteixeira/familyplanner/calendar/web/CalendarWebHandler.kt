package com.luisteixeira.familyplanner.calendar.web

import com.luisteixeira.familyplanner.auth.currentUserId
import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.utils.parseDate
import com.luisteixeira.familyplanner.calendar.utils.parseTime
import com.luisteixeira.familyplanner.calendar.utils.parseZone
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.core.web.escapeHtml
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.hxRedirect
import com.luisteixeira.familyplanner.core.web.isHtmx
import com.luisteixeira.familyplanner.core.web.redirectSeeOther
import com.luisteixeira.familyplanner.core.web.respondHtml
import com.luisteixeira.familyplanner.household.householdId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.resourceClasspathResource
import io.ktor.server.request.receiveParameters
import java.time.LocalDate
import java.time.ZoneId

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
            service.createEvent(
                householdId,
                userId,
                EventInput(
                    title = form["title"].orEmpty(),
                    date = parseDate(form["date"].orEmpty()),
                    time = form["time"]?.takeIf { it.isNotBlank() }?.let { parseTime(it) },
                    timezone = form["timezone"]?.takeIf { it.isNotBlank() }?.let { parseZone(it) } ?: defaultTimezone,
                    description = form["description"],
                    address = form["address"],
                ),
            )
        } catch (e: DomainException) {
            call.respondHtml(
                """<div class="error">${e.message.orEmpty().escapeHtml()}</div>""",
                HttpStatusCode.UnprocessableEntity,
            )
            return
        }

        val weekUrl = "/app/households/$householdId/calendar" +
                "?week=${weekStart(event.date)}&timezone=${event.timezone.id}"

        if (call.isHtmx()) return call.hxRedirect(weekUrl)
        call.redirectSeeOther(weekUrl)
    }
}
