package com.luisteixeira.familyplanner.admin

import com.luisteixeira.familyplanner.auth.apikey.ApiKeyService
import com.luisteixeira.familyplanner.config.AppConfig
import com.luisteixeira.familyplanner.wiring.storage
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val name = args.firstOrNull()?.takeIf { it.isNotBlank() }
        ?: error("usage: createApiKey -PclientName=<client-name>")
    val config = AppConfig.fromEnvironment()

    config.persistence.storage(config.database).use { storage ->
        val (client, rawKey) = runBlocking { ApiKeyService(storage.apiClients).createClient(name) }
        println("Created API client '${client.name}' (id ${client.id})")
        println("API key (shown only once): $rawKey")
    }
}