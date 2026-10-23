package com.luisteixeira.familyplanner.calendar

import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.persistence.InMemoryCalendarRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CalendarServiceTest {

    private val lisbon = ZoneId.of("Europe/Lisbon")
    private val householdId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    private fun input(title: String = "Dentist", time: LocalTime? = null) = EventInput(
        title = title,
        date = LocalDate.of(2026, 10, 24),
        time = time,
        timezone = lisbon,
        description = null,
        address = null,
    )

    @Test
    fun createEventTrimsTitleAndKeepsHouseholdAndCreator() = runBlocking<Unit> {
        val service = CalendarService(InMemoryCalendarRepository())

        val event = service.createEvent(householdId, userId, input(title = "  Dentist  "))

        assertEquals("Dentist", event.title)
        assertEquals(householdId, event.householdId)
        assertEquals(userId, event.createdBy)
    }

    @Test
    fun updateChangesFieldsAndKeepsIdAndCreator() = runBlocking<Unit> {
        val service = CalendarService(InMemoryCalendarRepository())
        val created = service.createEvent(householdId, userId, input())

        val updated = service.updateEvent(householdId, created.id, input(title = "Doctor", time = LocalTime.of(9, 30)))

        assertEquals(created.id, updated.id)
        assertEquals("Doctor", updated.title)
        assertEquals(LocalTime.of(9, 30), updated.time)
        assertEquals(userId, updated.createdBy)
        assertEquals(LocalDate.of(2026, 10, 24).atTime(9, 30).atZone(lisbon).toInstant(), updated.startsAt)
    }

    @Test
    fun updateFromAnotherHouseholdIsRejected() = runBlocking<Unit> {
        val service = CalendarService(InMemoryCalendarRepository())
        val created = service.createEvent(householdId, userId, input())

        val error = assertFailsWith<DomainException> {
            service.updateEvent(UUID.randomUUID(), created.id, input(title = "Hijack"))
        }

        assertEquals("event not found", error.message)
    }

    @Test
    fun deleteRemovesTheEvent() = runBlocking<Unit> {
        val repo = InMemoryCalendarRepository()
        val service = CalendarService(repo)
        val created = service.createEvent(householdId, userId, input())

        service.deleteEvent(householdId, created.id)

        assertNull(repo.findEvent(created.id))
    }

    @Test
    fun deleteFromAnotherHouseholdIsRejected() = runBlocking<Unit> {
        val service = CalendarService(InMemoryCalendarRepository())
        val created = service.createEvent(householdId, userId, input())

        assertFailsWith<DomainException> {
            service.deleteEvent(UUID.randomUUID(), created.id)
        }
    }
}
