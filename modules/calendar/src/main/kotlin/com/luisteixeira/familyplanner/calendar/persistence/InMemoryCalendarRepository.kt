package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event
import java.util.UUID

class InMemoryCalendarRepository: CalendarRepository {
    private val lock = Any()
    private val events = HashMap<UUID, Event>()

    override suspend fun saveEvent(event: Event) {
        synchronized(lock) {
            events[event.id] = event
        }
    }
}