package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event
import io.ktor.events.Events
import io.ktor.http.content.EntityTagVersion
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.time
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class PostgresCalendarRepository(
    private val database: Database
) : CalendarRepository{

    override suspend fun saveEvent(event: Event) {
        dbTransaction(database) {
            val updated = EventsTable.update({ EventsTable.id eq event.id }) {
                it[householdId] = event.householdId
                it[createdBy] = event.createdBy
                it[title] = event.title
                it[eventDate] = event.date
                it[eventTime] = event.time
                it[timezone] = event.timezone.id
                it[startsAt] = event.startsAt.atOffset(ZoneOffset.UTC)
                it[description] = event.description
                it[address] = event.address
            }
            if (updated == 0) {
                EventsTable.insert {
                    it[id] = event.id
                    it[householdId] = event.householdId
                    it[createdBy] = event.createdBy
                    it[title] = event.title
                    it[eventDate] = event.date
                    it[eventTime] = event.time
                    it[timezone] = event.timezone.id
                    it[startsAt] = event.startsAt.atOffset(ZoneOffset.UTC)
                    it[description] = event.description
                    it[address] = event.address
                    it[createdAt] = event.createdAt.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findEvent(eventId: UUID): Event? {
        return dbTransaction(database) {
            EventsTable.selectAll().where { EventsTable.id eq eventId }
                .singleOrNull()?.toEvent()
        }
    }

    override suspend fun deleteEvent(eventId: UUID) {
        dbTransaction(database) {
            EventsTable.deleteWhere { EventsTable.id eq eventId }
        }
    }

    override suspend fun listByHousehold(householdId: UUID, from: Instant, to: Instant): List<Event> {
        return dbTransaction(database) {
            EventsTable.selectAll()
                .where {
                    (EventsTable.householdId eq householdId ) and
                            (EventsTable.startsAt greaterEq from.atOffset(ZoneOffset.UTC)) and
                            (EventsTable.startsAt less to.atOffset(ZoneOffset.UTC))
                }
                .orderBy(EventsTable.startsAt)
                .map { it.toEvent() }
        }
    }

    override suspend fun findNextEvent(householdId: UUID, from: Instant): Event? {
        return dbTransaction(database) {
            EventsTable.selectAll()
                .where {
                    (EventsTable.householdId eq householdId) and
                            (EventsTable.startsAt greaterEq from.atOffset(ZoneOffset.UTC))
                }
                .orderBy(EventsTable.startsAt)
                .limit(1)
                .singleOrNull()
                ?.toEvent()
        }
    }

    private fun ResultRow.toEvent(): Event = Event(
        id = this[EventsTable.id],
        householdId = this[EventsTable.householdId],
        createdBy = this[EventsTable.createdBy],
        title = this[EventsTable.title],
        date = this[EventsTable.eventDate],
        time = this[EventsTable.eventTime],
        timezone = ZoneId.of(this[EventsTable.timezone]),
        startsAt = this[EventsTable.startsAt].toInstant(),
        description = this[EventsTable.description],
        address = this[EventsTable.address],
        createdAt = this[EventsTable.createdAt].toInstant(),

    )

    private object EventsTable : Table("calendar.events") {
        val id = javaUUID("id")
        val householdId = javaUUID("household_id")
        val createdBy = javaUUID("created_by")
        val title = text("title")
        val eventDate = date("event_date")
        val eventTime = time("event_time").nullable()
        val timezone = text("timezone")
        val startsAt = timestampWithTimeZone("starts_at")
        val description = text("description").nullable()
        val address = text("address").nullable()
        val createdAt = timestampWithTimeZone("created_at")

        override val primaryKey = PrimaryKey(id)
    }

}