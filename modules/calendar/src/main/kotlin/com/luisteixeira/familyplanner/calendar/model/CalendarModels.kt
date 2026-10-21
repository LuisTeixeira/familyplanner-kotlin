package com.luisteixeira.familyplanner.calendar.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.time.Instant

data class Event(
    val id: UUID,
    val householdId: UUID,
    val createdBy: UUID,
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val timezone: ZoneId,
    val startsAt: Instant,
    val description: String?,
    val address: String?,
    val createdAt: Instant,
)

data class EventInput(
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val timezone: ZoneId,
    val description: String?,
    val address: String?,
)

data class CalendarSummary(
    val nextEvent: Event?,
    val eventsNextWeek: Int,
)