package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.persistence.InMemoryCalendarRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarServiceTest {

    @Test
    fun createEventTrimsTitleAndKeepsHouseholdAndCreator() = runBlocking<Unit> {
        val service = CalendarService(InMemoryCalendarRepository())
        val householdId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        val event = service.createEvent(
            householdId,
            userId,
            EventInput(
                title = " Doctor Appointment ",
                date = LocalDate.of(2026, 10, 21),
                time = null,
                timezone = ZoneId.of("Europe/Berlin"),
                description = null,
                address = null
            ),
        )

        assertEquals("Doctor Appointment", event.title)
        assertEquals(householdId, event.householdId)
        assertEquals(userId, event.createdBy)
    }
}