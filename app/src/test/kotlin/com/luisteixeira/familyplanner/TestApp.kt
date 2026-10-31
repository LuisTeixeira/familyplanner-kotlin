package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.apikey.API_KEY_HEADER
import com.luisteixeira.familyplanner.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.server.testing.testApplication

fun withApp(config: AppConfig = AppConfig(), block: suspend (HttpClient) -> Unit) = testApplication {
    val app = composeApp(config)
    application { familyPlannerApp(app) }
    val apiKey = app.apiKeys.createClient("test-client").second
    block(createClient { install(DefaultRequest) { headers.append(API_KEY_HEADER, apiKey) } })
}
