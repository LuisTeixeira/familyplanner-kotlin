package com.luisteixeira.familyplanner.auth.tokens

import com.auth0.jwt.JWT
import java.security.KeyPairGenerator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AccessTokenIssuerTest {

    private val keys = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val start = Instant.parse("2026-06-01T12:00:00Z")

    @Test
    fun tokenIsIssuedNowAndExpiresAfterTheLifetime() {
        val issuer = AccessTokensIssuer(
            keys = keys,
            issuer = "familyplanner",
            audience = "familyplanner-api",
            clock = Clock.fixed(start, ZoneId.of("UTC")),
        )

        val decoded = JWT.decode(issuer.issue(UUID.randomUUID()))

        assertEquals(start, decoded.issuedAt.toInstant())
        assertEquals(start.plus(Duration.ofMinutes(15)), decoded.expiresAt.toInstant())
    }
}