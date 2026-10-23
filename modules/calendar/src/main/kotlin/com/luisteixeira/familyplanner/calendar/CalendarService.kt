package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.calendar.model.CalendarSummary
import com.luisteixeira.familyplanner.calendar.model.Event
import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.persistence.CalendarRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.util.UUID
import java.time.Instant

class CalendarService(
    private val repo: CalendarRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun createEvent(householdId: UUID, userId: UUID, input: EventInput): Event {
        val fields = normalize(input)
        val event = Event(
            id = UUID.randomUUID(),
            householdId = householdId,
            createdBy = userId,
            title = fields.title,
            date = fields.date,
            time = fields.time,
            timezone = fields.timezone,
            startsAt = fields.startsAt(),
            description = fields.description,
            address = fields.address,
            createdAt = clock.instant()
        )
        repo.saveEvent(event)
        return event
    }

    suspend fun getEvent(householdId: UUID, eventId: UUID): Event {
        return requireEvent(householdId, eventId)
    }

    suspend fun updateEvent(householdId: UUID, eventId: UUID, input: EventInput): Event {
        val existing = requireEvent(householdId, eventId)
        val fields = normalize(input)
        val updated = existing.copy(
            title = fields.title,
            date = fields.date,
            time =  fields.time,
            timezone = fields.timezone,
            startsAt = fields.startsAt(),
            description = fields.description,
            address = fields.address,
        )
        repo.saveEvent(updated)
        return updated
    }

    suspend fun deleteEvent(householdId: UUID, eventId: UUID) {
        requireEvent(householdId, eventId)
        repo.deleteEvent(eventId)
    }

    suspend fun listEvents(householdId: UUID, from: Instant, to: Instant): List<Event> {
        if (!from.isBefore(to)) {
            throw DomainException("from must be before to")
        }
        if (Duration.between(from, to) > MAX_RANGE) {
            throw DomainException("range must be at most 366 days")
        }

        return repo.listByHousehold(householdId, from, to)
    }

    suspend fun summary(householdId: UUID): CalendarSummary {
        val now = clock.instant()
        val weekEnd = now.plus(Duration.ofDays(7))

        return CalendarSummary(
            nextEvent = repo.findNextEvent(householdId, now),
            eventsNextWeek = repo.listByHousehold(householdId, now, weekEnd).size,
        )
    }

    private suspend fun requireEvent(householdId: UUID, eventId: UUID): Event {
        val event = repo.findEvent(eventId)
        if (event == null || event.householdId != householdId) {
            throw DomainException("event not found")
        }
        return event
    }

    private fun normalize(input: EventInput): EventInput {
        val title = input.title.trim()
        if (title.isEmpty()) {
            throw DomainException("event title is required")
        }
        if (title.length > TITLE_MAX) {
            throw DomainException("event title must not be longer than $TITLE_MAX characters")
        }

        val description = input.description.blankToNull()
        if (description != null && description.length > DESCRIPTION_MAX) {
            throw DomainException("event description must not be longer than $DESCRIPTION_MAX characters")
        }

        val address = input.address.blankToNull()
        if (address != null && address.length > ADDRESS_MAX) {
            throw DomainException("event address must not be longer than $ADDRESS_MAX characters")
        }

        return input.copy(title = title, description = description, address = address)
    }

    private fun EventInput.startsAt(): Instant =
        date.atTime(time ?: LocalTime.MIDNIGHT).atZone(timezone).toInstant()

    private companion object {
        const val TITLE_MAX = 200
        const val DESCRIPTION_MAX = 2000
        const val ADDRESS_MAX = 300
        val MAX_RANGE: Duration = Duration.ofDays(366)
    }
}

private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }