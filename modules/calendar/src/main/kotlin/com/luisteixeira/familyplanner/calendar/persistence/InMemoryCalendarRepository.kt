package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event
import java.time.Instant
import java.util.UUID

class InMemoryCalendarRepository: CalendarRepository {
    private val lock = Any()
    private val events = HashMap<UUID, Event>()

    override suspend fun saveEvent(event: Event) {
        synchronized(lock) {
            events[event.id] = event
        }
    }

    override suspend fun findEvent(eventId: UUID): Event? {
        synchronized(lock) {
            return events[eventId]
        }
    }

    override suspend fun deleteEvent(eventId: UUID) {
        synchronized(lock) {
            events.remove(eventId)
        }
    }

    override suspend fun listByHousehold(householdId: UUID, from: Instant, to: Instant): List<Event> {
        synchronized(lock) {
            return events.values
                .filter { it.householdId == householdId && it.startsAt >= from && it.startsAt < to}
                .sortedBy { it.startsAt }
        }
    }

    override suspend fun findNextEvent(householdId: UUID, from: Instant): Event? {
        synchronized(lock) {
            return events.values
                .filter { it.householdId == householdId && it.startsAt >= from }
                .minByOrNull { it.startsAt }
        }
    }
}