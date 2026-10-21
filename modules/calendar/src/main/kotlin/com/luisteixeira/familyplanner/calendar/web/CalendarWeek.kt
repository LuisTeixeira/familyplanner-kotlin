package com.luisteixeira.familyplanner.calendar.web

import com.luisteixeira.familyplanner.calendar.model.Event
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class DayView(
    val date: LocalDate,
    val events: List<Event>
)

data class WeekView(
    val start: LocalDate,
    val previous: LocalDate,
    val next: LocalDate,
    val days: List<DayView>,
)

fun weekStart(day: LocalDate): LocalDate =
    day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun weekView(start: LocalDate, zone: ZoneId, events: List<Event>): WeekView {
    val eventsByDay = events.groupBy { it.startsAt.atZone(zone).toLocalDate() }

    return WeekView(
        start = start,
        previous = start.minusWeeks(1),
        next = start.plusWeeks(1),
        days = (0L..6L).map { offset ->
            val day = start.plusDays(offset)
            DayView(day, eventsByDay[day].orEmpty())
        },
    )
}