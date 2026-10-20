package com.luisteixeira.familyplanner.auth

import com.luisteixeira.familyplanner.auth.persistence.InMemoryAuthRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AuthServiceTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun clockAt(instant: Instant) = Clock.fixed(instant, ZoneOffset.UTC)

    @Test
    fun registerRejectsDuplicateEmail() = runBlocking<Unit> {
        val service = AuthService(InMemoryAuthRepository(), clockAt(now))
        service.register("ann@example.com", "secret", "Ann")

        val error = assertFailsWith<DomainException> {
            service.register("ann@example.com", "other", "Ann Again")
        }

        assertEquals("user already exists", error.message)
    }

    @Test
    fun loginRejectsWrongPassword() = runBlocking<Unit> {
        val service = AuthService(InMemoryAuthRepository(), clockAt(now))
        service.register("ann@example.com", "secret", "Ann")

        assertFailsWith<DomainException> { service.login("ann@example.com", "wrong") }
    }

    @Test
    fun sessionExpiresAfterSevenDays() = runBlocking<Unit> {
        val repo = InMemoryAuthRepository()
        val issuing = AuthService(repo, clockAt(now))
        val session = issuing.register("ann@example.com", "secret", "Ann").session

        val later = AuthService(repo, clockAt(now.plus(Duration.ofDays(8))))

        assertFailsWith<DomainException> { later.validateSession(session.token) }
        assertNull(repo.findSession(session.token))
    }

    @Test
    fun findUserIdByEmailReturnsRegisteredUser() = runBlocking<Unit> {
        val service = AuthService(InMemoryAuthRepository(), clockAt(now))
        val userId = service.register("ann@example.com", "secret", "Ann").user.id

        assertEquals(userId, service.findUserIdByEmail("ann@example.com"))
        assertNull(service.findUserIdByEmail("missing@example.com"))
    }
}
