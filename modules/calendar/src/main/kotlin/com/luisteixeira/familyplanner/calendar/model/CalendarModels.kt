package com.luisteixeira.familyplanner.calendar.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.time.Instant
import java.time.ZonedDateTime

data class Event(
    val id: UUID,
    val householdId: UUID,
    val createdBy: UUID,
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val timezone: ZoneId,
    val description: String?,
    val address: String?,
    val createdAt: Instant,
) {
    fun startsAt(): ZonedDateTime = date.atTime(time ?: LocalTime.MIDNIGHT).atZone(timezone)
}

data class EventInput(
    val title: String,
    val date: LocalDate,
    val time: LocalTime?,
    val timezone: ZoneId,
    val description: String?,
    val address: String?,
)