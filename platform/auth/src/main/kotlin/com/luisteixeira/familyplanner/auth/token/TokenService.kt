package com.luisteixeira.familyplanner.auth.token

import com.luisteixeira.familyplanner.core.domain.DomainException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.util.Base64
import java.util.HexFormat
import java.util.UUID


data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)

class TokenService(
    private val refreshTokens: RefreshTokenRepository,
    private val accessTokens: AccessTokensIssuer,
    private val clock: Clock = Clock.systemUTC(),
    private val refreshLifeTime: Duration = Duration.ofDays(30),
) {

    private val random = SecureRandom()

    suspend fun issue(userId: UUID): TokenPair =
        issueInFamily(userId, UUID.randomUUID())

    suspend fun refresh(rawRefreshToken: String): TokenPair {
        val tokenHash = hash(rawRefreshToken)
        val stored = refreshTokens.findByHash(hash(rawRefreshToken)) ?: throw invalidToken()
        val now = clock.instant()

        if (now.isAfter(stored.expiresAt)) throw invalidToken()

        if (!refreshTokens.revokeIfActive(tokenHash, now)) {
            refreshTokens.revokeFamily(stored.familyId, now)
            throw invalidToken()
        }
        return issueInFamily(stored.userId, stored.familyId)
    }

    suspend fun logout(rawRefreshToken: String) {
        val stored = refreshTokens.findByHash(hash(rawRefreshToken)) ?: return
        refreshTokens.revokeFamily(stored.familyId, clock.instant())
    }

    private suspend fun issueInFamily(userId: UUID, familyId: UUID): TokenPair {
        val rawRefreshToken = "rt_" + Base64.getUrlEncoder()
            .withoutPadding().encodeToString(ByteArray(32).also { random.nextBytes(it) })
        val now = clock.instant()
        refreshTokens.saveToken(
            RefreshToken(
                tokenHash = hash(rawRefreshToken),
                userId = userId,
                familyId = familyId,
                expiresAt = now.plus(refreshLifeTime),
                revokedAt = null,
            ),
        )
        return TokenPair(accessTokens.issue(userId), rawRefreshToken)
    }

    private fun invalidToken() = DomainException("invalid refresh token")

    private fun hash(value: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toByteArray()))
}
