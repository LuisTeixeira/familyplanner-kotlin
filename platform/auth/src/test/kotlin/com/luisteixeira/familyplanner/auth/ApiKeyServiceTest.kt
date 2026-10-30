package com.luisteixeira.familyplanner.auth

import com.luisteixeira.familyplanner.auth.persistence.InMemoryApiClientRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ApiKeyServiceTest {

    @Test
    fun createdKeyAuthenticates() = runBlocking {
        val service = ApiKeyService(InMemoryApiClientRepository())
        val (client, key) = service.createClient("Mobile")

        assertEquals(client.id, service.authenticate(key)?.id)
    }

    @Test
    fun unknownIsRejected() = runBlocking {
        val service = ApiKeyService(InMemoryApiClientRepository())
        service.createClient("Mobile")

        assertNull(service.authenticate("fp_unknown"))
    }

    @Test
    fun revokedKeyIsRejected() = runBlocking {
        val service = ApiKeyService(InMemoryApiClientRepository())
        val (client, key) = service.createClient("Mobile")

        service.revoke(client)

        assertNull(service.authenticate(key))
    }

    @Test
    fun rawKeyNeverStored() = runBlocking {
        val service = ApiKeyService(InMemoryApiClientRepository())
        val (client, key) = service.createClient("Mobile")

        assertNotEquals(key, client.keyHash)
    }
}