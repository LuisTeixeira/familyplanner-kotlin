package com.luisteixeira.familyplanner.auth.apikey

import com.luisteixeira.familyplanner.core.web.HttpError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin

const val API_KEY_HEADER = "X-API-Key"

class ApiKeyGuardConfig {
    lateinit var service: ApiKeyService
}

val ApiKeyGuard = createRouteScopedPlugin("ApiKeyGuard", ::ApiKeyGuardConfig) {
    val service = pluginConfig.service

    onCall { call ->
        val rawKey = call.request.headers[API_KEY_HEADER]
        if (rawKey == null || service.authenticate(rawKey) == null) {
            throw HttpError(HttpStatusCode.Unauthorized, "Unauthorized")
        }
    }
}
