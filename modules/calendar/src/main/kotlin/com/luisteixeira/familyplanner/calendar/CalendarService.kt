package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.calendar.model.Event
import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.persistence.CalendarRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import java.time.Clock
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

    private companion object {
        const val TITLE_MAX = 200
        const val DESCRIPTION_MAX = 2000
        const val ADDRESS_MAX = 300
    }
}

private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }