package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event
import java.time.Instant
import java.util.UUID

interface CalendarRepository {
    suspend fun saveEvent(event: Event)
    suspend fun listByHousehold(householdId: UUID, from: Instant, to: Instant): List<Event>
    suspend fun findNextEvent(householdId: UUID, from: Instant): Event?
}