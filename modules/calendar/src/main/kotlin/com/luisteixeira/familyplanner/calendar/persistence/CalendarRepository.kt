package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event
import java.util.UUID

interface CalendarRepository {
    suspend fun saveEvent(event: Event)
    suspend fun listByHousehold(householdId: UUID): List<Event>
}