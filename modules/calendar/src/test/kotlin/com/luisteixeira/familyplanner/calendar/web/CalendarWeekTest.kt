package com.luisteixeira.familyplanner.calendar.web

import com.luisteixeira.familyplanner.calendar.model.Event
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CalendarWeekTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun event(title: String, at: LocalDateTime, zone: ZoneId) = Event(
        id = UUID.randomUUID(),
        householdId = UUID.randomUUID(),
        createdBy = UUID.randomUUID(),
        title = title,
        date = at.toLocalDate(),
        time = at.toLocalTime(),
        timezone = zone,
        startsAt = at.atZone(zone).toInstant(),
        description = null,
        address = null,
        createdAt = at.atZone(zone).toInstant(),
    )

    @Test
    fun weekStartIsMonday() {
        assertEquals(LocalDate.of(2026, 10, 19), weekStart(LocalDate.of(2026, 10, 21)))
        assertEquals(LocalDate.of(2026, 10, 19), weekStart(LocalDate.of(2026, 10, 25)))
        assertEquals(LocalDate.of(2026, 10, 19), weekStart(LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun weekHasSevenDaysAndNavigateByWeek() {
        val start = LocalDate.of(2026, 10, 19)

        val week = weekView(start, berlin, emptyList())

        assertEquals(7, week.days.size)
        assertEquals(start, week.days.first().date)
        assertEquals(start.plusDays(6), week.days.last().date)
        assertEquals(start.minusWeeks(1), week.previous)
        assertEquals(start.plusWeeks(1), week.next)
    }

    @Test
    fun eventsAreGroupedByDayInViewerZone() {
        val start = LocalDate.of(2026, 10, 19)
        val lateInBerlin = event("Late dinner", LocalDateTime.of(2026, 10, 24, 23, 30), berlin)

        val inBerlin = weekView(start, berlin, listOf(lateInBerlin))
        val inTokyo = weekView(start, tokyo, listOf(lateInBerlin))

        assertEquals(listOf(lateInBerlin), inBerlin.days.single{ it.date == LocalDate.of(2026, 10, 24) }.events)
        assertTrue(inTokyo.days.single { it.date == LocalDate.of(2026, 10, 24) }.events.isEmpty())
        assertEquals(listOf(lateInBerlin), inTokyo.days.single{ it.date == LocalDate.of(2026, 10, 25) }.events)
    }
}