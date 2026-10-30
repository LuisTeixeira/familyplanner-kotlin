package com.luisteixeira.familyplanner.auth

import com.luisteixeira.familyplanner.auth.model.ApiClient
import com.luisteixeira.familyplanner.auth.persistence.ApiClientRepository
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

class ApiKeyService(
    private val repo: ApiClientRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val random = SecureRandom()

    suspend fun createClient(name: String): Pair<ApiClient, String> {
        val rawKey = "fp_" + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also { random.nextBytes(it) })
        val client = ApiClient(
            id = UUID.randomUUID(),
            name = name,
            keyHash = hash(rawKey),
            createdAt = clock.instant(),
            revokedAt = null,
        )
        repo.saveClient(client)
        return client to rawKey
    }

    suspend fun authenticate(rawKey: String): ApiClient? {
        val client = repo.findByKeyHash(hash(rawKey)) ?: return null
        return client.takeIf { it.revokedAt == null }
    }

    suspend fun revoke(client: ApiClient) {
        repo.saveClient(client.copy(revokedAt = clock.instant()))
    }

    private fun hash(rawKey: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(rawKey.toByteArray()))

}