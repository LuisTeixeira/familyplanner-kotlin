package com.luisteixeira.familyplanner.auth.users

import com.luisteixeira.familyplanner.core.domain.DomainException
import kotlinx.coroutines.runBlocking
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class UserServiceTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun clockAt(instant: Instant) = Clock.fixed(instant, ZoneOffset.UTC)

    @Test
    fun registerRejectsDuplicateEmail() = runBlocking<Unit> {
        val service = UserService(InMemoryUserRepository(), clockAt(now))
        service.register("ann@example.com", "secret", "Ann")

        val error = assertFailsWith<DomainException> {
            service.register("ann@example.com", "other", "Ann Again")
        }

        assertEquals("user already exists", error.message)
    }

    @Test
    fun authenticateRejectsWrongPassword() = runBlocking<Unit> {
        val service = UserService(InMemoryUserRepository(), clockAt(now))
        service.register("ann@example.com", "secret", "Ann")

        assertFailsWith<DomainException> { service.authenticate("ann@example.com", "wrong") }
    }

    @Test
    fun findUserIdByEmailReturnsRegisteredUser() = runBlocking<Unit> {
        val service = UserService(InMemoryUserRepository(), clockAt(now))
        val userId = service.register("ann@example.com", "secret", "Ann").id

        assertEquals(userId, service.findUserIdByEmail("ann@example.com"))
        assertNull(service.findUserIdByEmail("missing@example.com"))
    }
}
