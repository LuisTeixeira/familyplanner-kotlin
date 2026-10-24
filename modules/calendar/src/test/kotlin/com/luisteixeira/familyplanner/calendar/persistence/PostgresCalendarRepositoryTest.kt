package com.luisteixeira.familyplanner.calendar.persistence

import com.luisteixeira.familyplanner.calendar.model.Event
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresDatabase
import com.luisteixeira.familyplanner.core.persistence.postgres.connectPostgres
import com.luisteixeira.familyplanner.core.persistence.postgres.migrate
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgresCalendarRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply{
            start()
        }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "calendar", module = "calendar")
            }
        }
    }

    private val repo by lazy { PostgresCalendarRepository(postgres.database) }
    private val utc = ZoneId.of("UTC")
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val householdId = UUID.randomUUID()

    @BeforeTest
    fun cleanTable() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use { it.execute("TRUNCATE calendar.events") }
        }
    }

    private fun event(
        date: LocalDate,
        time: LocalTime? = null,
        title: String = "Event",
        household: UUID = householdId,
    ): Event = Event(
        id = UUID.randomUUID(),
        householdId = household,
        createdBy = UUID.randomUUID(),
        title = title,
        date = date,
        time = time,
        timezone = utc,
        startsAt = date.atTime(time ?: LocalTime.MIDNIGHT).atZone(utc).toInstant(),
        description = null,
        address = null,
        createdAt = createdAt,
    )

    private fun startOf(day: LocalDate): Instant = day.atStartOfDay(utc).toInstant()

    @Test
    fun savedEventCanBeFound() = runBlocking {
        val event = event(LocalDate.of(2026, 10, 24), LocalTime.of(10, 30), "Dentist")
            .copy(description = "Bring card", address = "Main Street 1")

        repo.saveEvent(event)

        assertEquals(event, repo.findEvent(event.id))
    }

    @Test
    fun allDayEventKeepsNoTime() = runBlocking {
        val event = event(LocalDate.of(2026, 10, 24), null, "Holiday")

        repo.saveEvent(event)

        assertNull(repo.findEvent(event.id)?.time)
    }

    @Test
    fun updateMovesTheStoredEvent() = runBlocking {
        val original = event(LocalDate.of(2026, 10, 24), LocalTime.of(10, 0), "Dentist")
        repo.saveEvent(original)
        val moved = original.copy(
            title = "Doctor",
            date = LocalDate.of(2026, 10, 25),
            time = LocalTime.of(9, 0),
            startsAt = LocalDate.of(2026, 10, 25).atTime(9, 0).atZone(utc).toInstant(),
        )

        repo.saveEvent(moved)

        assertEquals(moved, repo.findEvent(original.id))
    }

    @Test
    fun deleteRemovesTheEvent() = runBlocking {
        val event = event(LocalDate.of(2026, 10, 24))
        repo.saveEvent(event)

        repo.deleteEvent(event.id)

        assertNull(repo.findEvent(event.id))
    }

    @Test
    fun rangeIncludesStartAndExcludesEnd() = runBlocking {
        val first = event(LocalDate.of(2026, 10, 24), LocalTime.of(10, 0), "First")
        val onBoundary = event(LocalDate.of(2026, 10, 25), null, "Boundary")
        repo.saveEvent(first)
        repo.saveEvent(onBoundary)

        val events = repo.listByHousehold(householdId, startOf(LocalDate.of(2026, 10, 24)), startOf(LocalDate.of(2026, 10, 25)))

        assertEquals(listOf(first), events)
    }

    @Test
    fun listIsOrderedByStart() = runBlocking {
        val late = event(LocalDate.of(2026, 10, 24), LocalTime.of(18, 0), "Late")
        val early = event(LocalDate.of(2026, 10, 24), LocalTime.of(8, 0), "Early")
        repo.saveEvent(late)
        repo.saveEvent(early)

        val events = repo.listByHousehold(householdId, startOf(LocalDate.of(2026, 10, 24)), startOf(LocalDate.of(2026, 10, 25)))

        assertEquals(listOf(early, late), events)
    }

    @Test
    fun otherHouseholdsDoNotSeeTheEvents() = runBlocking {
        repo.saveEvent(event(LocalDate.of(2026, 10, 24), LocalTime.of(10, 0)))

        val events = repo.listByHousehold(UUID.randomUUID(), startOf(LocalDate.of(2026, 10, 24)), startOf(LocalDate.of(2026, 10, 25)))

        assertEquals(emptyList(), events)
    }

    @Test
    fun nextEventIsTheFirstUpcomingOne() = runBlocking {
        val past = event(LocalDate.of(2026, 10, 20), LocalTime.of(10, 0), "Past")
        val next = event(LocalDate.of(2026, 10, 24), LocalTime.of(10, 0), "Next")
        val later = event(LocalDate.of(2026, 10, 30), null, "Later")
        repo.saveEvent(past)
        repo.saveEvent(next)
        repo.saveEvent(later)

        assertEquals(next, repo.findNextEvent(householdId, startOf(LocalDate.of(2026, 10, 22))))
    }

    @Test
    fun nextEventIsNullWhenNothingIsUpcoming() = runBlocking {
        repo.saveEvent(event(LocalDate.of(2026, 10, 20), LocalTime.of(10, 0), "Past"))

        assertNull(repo.findNextEvent(householdId, startOf(LocalDate.of(2026, 10, 22))))
    }
}