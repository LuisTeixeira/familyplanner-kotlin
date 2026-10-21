package com.luisteixeira.familyplanner.calendar.utils

import com.luisteixeira.familyplanner.core.domain.DomainException
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

fun parseDate(value: String): LocalDate {
    if (value.isBlank()) {
        throw DomainException("event date is required")
    }
    return try {
        LocalDate.parse(value)
    } catch (e: DateTimeParseException) {
        throw DomainException("date must use the format YYYY-MM-DD")
    }
}

fun parseTime(value: String): LocalTime {
    return try {
        LocalTime.parse(value)
    } catch (e: DateTimeParseException) {
        throw DomainException("time must use the format HH:mm")
    }
}

fun parseZone(value: String): ZoneId {
    return try {
        ZoneId.of(value)
    } catch (e: DateTimeException) {
        throw DomainException("unknown timezone: $value")
    }
}