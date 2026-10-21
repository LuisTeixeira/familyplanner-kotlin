package com.luisteixeira.familyplanner.calendar.dto

import com.luisteixeira.familyplanner.calendar.model.Event
import com.luisteixeira.familyplanner.calendar.model.EventInput
import com.luisteixeira.familyplanner.calendar.utils.parseDate
import com.luisteixeira.familyplanner.calendar.utils.parseTime
import com.luisteixeira.familyplanner.calendar.utils.parseZone
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.serialization.InstantSerializer
import com.luisteixeira.familyplanner.core.serialization.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.UUID

@Serializable
data class CreateEventRequest(
    val title: String = "",
    val date: String = "",
    val time: String? = "",
    val timezone: String? = "",
    val description: String? = "",
    val address: String? = "",
) {
    fun toInput(defaultTimezone: ZoneId): EventInput = EventInput(
        title = title,
        date = parseDate(date),
        time = time?.let { parseTime(it) },
        timezone = timezone?.let { parseZone(it) } ?: defaultTimezone,
        description = description,
        address = address
    )
}

@Serializable
data class  EventDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("household_id")
    val householdId: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("created_by")
    val createdBy: UUID,
    val title: String,
    val date: String,
    val time: String?,
    val timezone: String,
    @Serializable(with = InstantSerializer::class)
    @SerialName("starts_at")
    val startsAt: Instant,
    val description: String?,
    val address: String?,
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant
)

fun Event.toDto(): EventDto = EventDto(
    id = id,
    householdId = householdId,
    createdBy = createdBy,
    title = title,
    date = date.toString(),
    time = time.toString(),
    timezone = timezone.id,
    startsAt = startsAt,
    description = description,
    address = address,
    createdAt = createdAt,
)

data class EventRange(
    val from: Instant,
    val to: Instant,
)

fun eventRange(from: String?, to: String?, timezone: String?, defaultZone: ZoneId): EventRange {
    val zone = timezone?.let { parseZone(it) } ?: defaultZone
    val start = parseDate(required(from, "from")).atStartOfDay(zone).toInstant()
    val end = parseDate(required(to, "to")).plusDays(1).atStartOfDay(zone).toInstant()
    return EventRange(start, end)
}

private fun required(value: String?, name: String): String {
    if (value.isNullOrBlank()) {
        throw DomainException("$name is required")
    }
    return value
}