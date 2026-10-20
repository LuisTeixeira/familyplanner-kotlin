package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event

interface CalendarRepository {
    suspend fun saveEvent(event: Event)
}