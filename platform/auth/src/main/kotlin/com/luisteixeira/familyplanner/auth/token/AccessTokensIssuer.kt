package com.luisteixeira.familyplanner.auth.token

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import java.security.KeyPair
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Clock
import java.time.Duration
import java.util.Date
import java.util.UUID

class AccessTokensIssuer(
    keys: KeyPair,
    private val issuer: String,
    private val audience: String,
    private val clock: Clock = Clock.systemUTC(),
    val lifetime: Duration = Duration.ofMinutes(15),
) {

    private val algorithm = Algorithm.RSA256(keys.public as RSAPublicKey, keys.private as RSAPrivateKey)

    fun issue(userId: UUID): String {
        val now = clock.instant()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plus(lifetime)))
            .sign(algorithm)
    }
}

fun accessTokenVerifier(keys: KeyPair, issuer: String, audience: String): JWTVerifier =
    JWT.require(Algorithm.RSA256(keys.public as RSAPublicKey, null))
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

class RsaAccessTokenVerifier(
    keys: KeyPair,
    issuer: String,
    audience: String,
) : TokenVerifier {

    private val verifier = accessTokenVerifier(keys, issuer, audience)

    override fun verify(token: String): UUID? =
        try {
            UUID.fromString(verifier.verify(token).subject)
        } catch (e: JWTVerificationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}