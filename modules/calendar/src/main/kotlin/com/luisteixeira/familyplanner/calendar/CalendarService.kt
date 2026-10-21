package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.calendar.model.CalendarSummary
import com.luisteixeira.familyplanner.calendar.model.Event
import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.persistence.CalendarRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import java.time.Clock
import java.time.Duration
import java.util.UUID

class CalendarService(
    private val repo: CalendarRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun createEvent(householdId: UUID, userId: UUID, input: EventInput): Event {
        val title = input.title.trim()
        if (title.isEmpty()) {
            throw DomainException("event title required")
        }
        if (title.length > TITLE_MAX) {
            throw DomainException("event title must have less than $TITLE_MAX characters")
        }

        val description = input.description.blankToNull()
        if (description != null && description.length > DESCRIPTION_MAX) {
            throw DomainException("event description must have less than $DESCRIPTION_MAX characters")
        }

        val address = input.address.blankToNull()
        if (address != null && address.length > ADDRESS_MAX) {
            throw DomainException("event address must have less than $ADDRESS_MAX characters")
        }

        val event = Event(
            id = UUID.randomUUID(),
            householdId = householdId,
            createdBy = userId,
            title = title,
            date = input.date,
            time = input.time,
            timezone = input.timezone,
            description = description,
            address = address,
            createdAt = clock.instant()
        )
        repo.saveEvent(event)
        return event
    }

    suspend fun summary(householdId: UUID): CalendarSummary {
        val now = clock.instant()
        val weekEnd = now.plus(Duration.ofDays(7))

        val upcoming = repo.listByHousehold(householdId)
            .map { event -> event to event.startsAt()}
            .filter { (_, start) -> start.toInstant() >= now }
            .sortedBy { (_, start) -> start }

        return CalendarSummary(
            nextEvent = upcoming.firstOrNull()?.first,
            eventsNextWeek = upcoming.count() { (_, start) -> start.toInstant() < weekEnd }
        )
    }

    private companion object {
        const val TITLE_MAX = 200
        const val DESCRIPTION_MAX = 2000
        const val ADDRESS_MAX = 300
    }
}

private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }