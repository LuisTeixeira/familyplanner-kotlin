package com.luisteixeira.familyplanner.auth.session

import com.luisteixeira.familyplanner.core.domain.DomainException
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SessionServiceTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun clockAt(instant: Instant) = Clock.fixed(instant, ZoneOffset.UTC)

    @Test
    fun sessionExpiresAfterSevenDays() = runBlocking<Unit> {
        val repo = InMemorySessionRepository()
        val session = SessionService(repo, clockAt(now)).create(UUID.randomUUID())

        val later = SessionService(repo, clockAt(now.plus(Duration.ofDays(8))))

        assertFailsWith<DomainException> { later.validateSession(session.token) }
        assertNull(repo.findSession(session.token))
    }
}
