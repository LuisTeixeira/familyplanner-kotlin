package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.apikey.API_KEY_HEADER
import com.luisteixeira.familyplanner.auth.token.AccessTokensIssuer
import com.luisteixeira.familyplanner.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private fun withRawApp(config: AppConfig = AppConfig(), block: suspend (HttpClient, ComposedApp) -> Unit) = testApplication {
    val app = composeApp(config)
    application { familyPlannerApp(app) }
    block(createClient { }, app)
}

private suspend fun HttpClient.apiPost(path: String, apiKey: String, body: String, token: String? = null): HttpResponse =
    post(path) {
        contentType(ContentType.Application.Json)
        header(API_KEY_HEADER, apiKey)
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        setBody(body)
    }

private suspend fun HttpClient.apiGet(path: String, apiKey: String, token: String? = null): HttpResponse =
    get(path) {
        header(API_KEY_HEADER, apiKey)
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

private fun registration(email: String) =
    """{"email":"$email","password":"secret123","display_name":"User"}"""

private fun refreshBody(token: String) = """{"refresh_token":"$token"}"""

class AuthApiTest {

    @Test
    fun requestWithoutApiKeyIsUnauthorized() = withRawApp { client, _ ->
        val response = client.get("/api/v1/households")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun requestWithUnknownApiKeyIsUnauthorized() = withRawApp { client, _ ->
        val response = client.apiPost("/api/v1/register", "fp_unknown", registration("unknown-key@example.com"))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun revokedApiKeyIsUnauthorized() = withRawApp { client, app ->
        val (apiClient, apiKey) = app.apiKeys.createClient("revoked")
        app.apiKeys.revoke(apiClient)

        val response = client.apiPost("/api/v1/register", apiKey, registration("revoked-key@example.com"))

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun protectedEndpointWithoutAccessTokenIsUnauthorized() = withRawApp { client, app ->
        val apiKey = app.apiKeys.createClient("test-client").second

        val response = client.apiGet("/api/v1/households", apiKey)

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun protectedEndpointAcceptsValidAccessToken() = withRawApp { client, app ->
        val apiKey = app.apiKeys.createClient("test-client").second
        val accessToken = client.apiPost("/api/v1/register", apiKey, registration("valid-token@example.com"))
            .json().str("access_token")

        val response = client.apiGet("/api/v1/households", apiKey, accessToken)

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun expiredAccessTokenIsUnauthorized() {
        val config = AppConfig()
        withRawApp(config) { client, app ->
            val apiKey = app.apiKeys.createClient("test-client").second
            val expired = AccessTokensIssuer(
                config.jwt.keys,
                config.jwt.issuer,
                config.jwt.audience,
                lifetime = Duration.ofMinutes(-1),
            ).issue(UUID.randomUUID())

            val response = client.apiGet("/api/v1/households", apiKey, expired)

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun sessionCookieIsNotAcceptedAsBearerToken() = withRawApp { client, app ->
        val apiKey = app.apiKeys.createClient("test-client").second
        client.apiPost("/api/v1/register", apiKey, registration("session-bearer@example.com"))
        val sessionToken = client.webLogin("session-bearer@example.com")

        val response = client.apiGet("/api/v1/households", apiKey, sessionToken)

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun loginReturnsAccessAndRefreshTokens() = withRawApp { client, app ->
        val apiKey = app.apiKeys.createClient("test-client").second
        client.apiPost("/api/v1/register", apiKey, registration("login-tokens@example.com"))

        val body = client.apiPost(
            "/api/v1/login",
            apiKey,
            """{"email":"login-tokens@example.com","password":"secret123"}""",
        ).json()

        assertTrue(body.str("access_token").isNotEmpty())
        assertTrue(body.str("refresh_token").startsWith("rt_"))
        assertEquals("900", body.getValue("expires_in").jsonPrimitive.content)
    }

    @Test
    fun refreshRotatesTokensAndRejectsReuse() = withRawApp { client, app ->
        val apiKey = app.apiKeys.createClient("test-client").second
        val first = client.apiPost("/api/v1/register", apiKey, registration("rotate@example.com"))
            .json().str("refresh_token")

        val rotated = client.apiPost("/api/v1/token/refresh", apiKey, refreshBody(first))
        assertEquals(HttpStatusCode.OK, rotated.status)
        val second = rotated.json().str("refresh_token")
        assertNotEquals(first, second)

        val reused = client.apiPost("/api/v1/token/refresh", apiKey, refreshBody(first))
        assertEquals(HttpStatusCode.Unauthorized, reused.status)

        val afterReuse = client.apiPost("/api/v1/token/refresh", apiKey, refreshBody(second))
        assertEquals(HttpStatusCode.Unauthorized, afterReuse.status)
    }

    @Test
    fun logoutRevokesRefreshToken() = withRawApp { client, app ->
        val apiKey = app.apiKeys.createClient("test-client").second
        val registered = client.apiPost("/api/v1/register", apiKey, registration("logout@example.com")).json()
        val accessToken = registered.str("access_token")
        val refreshToken = registered.str("refresh_token")

        val logout = client.apiPost("/api/v1/logout", apiKey, refreshBody(refreshToken), accessToken)
        assertEquals(HttpStatusCode.OK, logout.status)

        val refreshed = client.apiPost("/api/v1/token/refresh", apiKey, refreshBody(refreshToken))
        assertEquals(HttpStatusCode.Unauthorized, refreshed.status)
    }
}
