package com.luisteixeira.familyplanner.auth.token

import com.luisteixeira.familyplanner.core.domain.DomainException
import kotlinx.coroutines.runBlocking
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

private class MutableClock(var now: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneId.of("UTC")
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = now
}

class TokenServiceTest {

    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val issuer = "familyplanner"
    private val audience = "familyplanner-api"
    private val userId = UUID.randomUUID()
    private val start = Instant.parse("2026-06-01T12:00:00Z")

    private fun newService(clock: MutableClock): TokenService = TokenService(
        refreshTokens = InMemoryRefreshTokenRepository(),
        accessTokens = AccessTokensIssuer(keys, issuer, audience),
        clock = clock,
    )

    @Test
    fun issuedAccessTokenVerifiesAndCarriesTheUser() = runBlocking<Unit> {
        val pair = newService(MutableClock(start)).issue(userId)

        val decoded = accessTokenVerifier(keys, issuer, audience).verify(pair.accessToken)

        assertEquals(userId.toString(), decoded.subject)
    }

    @Test
    fun refreshReturnsANewRefreshToken() = runBlocking<Unit> {
        val service = newService(MutableClock(start))
        val first = service.issue(userId)

        val second = service.refresh(first.refreshToken)

        assertNotEquals(first.refreshToken, second.refreshToken)
        service.refresh(second.refreshToken)
    }

    @Test
    fun reusingARotatedTokenRevokesTheWholeFamily() = runBlocking<Unit> {
        val service = newService(MutableClock(start))
        val first = service.issue(userId)
        val second = service.refresh(first.refreshToken)

        assertFailsWith<DomainException> { service.refresh(first.refreshToken) }
        assertFailsWith<DomainException> { service.refresh(second.refreshToken) }
    }

    @Test
    fun expiredRefreshTokenIsRejected() = runBlocking<Unit> {
        val clock = MutableClock(start)
        val service = newService(clock)
        val pair = service.issue(userId)

        clock.now = clock.now.plus(Duration.ofDays(31))

        assertFailsWith<DomainException> { service.refresh(pair.refreshToken) }
    }

    @Test
    fun logoutRevokesTheToken() = runBlocking<Unit> {
        val service = newService(MutableClock(start))
        val pair = service.issue(userId)

        service.logout(pair.refreshToken)

        assertFailsWith<DomainException> { service.refresh(pair.refreshToken) }
    }

    @Test
    fun logoutOfAnUnknownTokenDoesNothing() = runBlocking<Unit> {
        newService(MutableClock(start)).logout("rt_unknown")
    }
}
