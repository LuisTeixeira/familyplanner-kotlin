package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.csrf.CSRF_COOKIE
import com.luisteixeira.familyplanner.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

private fun withApp(config: AppConfig = AppConfig(), block: suspend (HttpClient) -> Unit) = testApplication {
    application { familyPlannerApp(composeApp(config)) }
    block(createClient { })
}

private fun setCookie(response: HttpResponse, name: String): String? =
    response.headers.getAll(HttpHeaders.SetCookie)?.firstOrNull { it.startsWith("$name=") }

private fun cookieValue(response: HttpResponse, name: String): String? =
    setCookie(response, name)?.let { Regex("$name=([^;]*)").find(it)?.groupValues?.get(1) }

private suspend fun HttpClient.register(email: String) {
    post("/api/v1/register") {
        contentType(ContentType.Application.Json)
        setBody("{\"email\":\"$email\",\"password\":\"secret123\",\"display_name\":\"User\"}")
    }
}

class LoginCsrfTest {

    @Test
    fun loginWithoutCsrfTokenIsForbidden() = withApp { client ->
        client.register("login-no-csrf@example.com")

        val response = client.post("/app/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("email=login-no-csrf@example.com&password=secret123")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun loginWithMatchingCsrfTokenSucceeds() = withApp { client ->
        client.register("login-ok@example.com")
        val csrf = assertNotNull(cookieValue(client.get("/app/login"), CSRF_COOKIE))

        val response = client.post("/app/login") {
            header(HttpHeaders.Cookie, "$CSRF_COOKIE=$csrf")
            header("X-CSRF-Token", csrf)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("email=login-ok@example.com&password=secret123")
        }

        assertEquals(HttpStatusCode.SeeOther, response.status)
    }

    @Test
    fun sessionCookieIsSecureByDefault() = withApp { client ->
        client.register("secure-session@example.com")
        val csrf = assertNotNull(cookieValue(client.get("/app/login"), CSRF_COOKIE))

        val response = client.post("/app/login") {
            header(HttpHeaders.Cookie, "$CSRF_COOKIE=$csrf")
            header("X-CSRF-Token", csrf)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("email=secure-session@example.com&password=secret123")
        }

        assertContains(assertNotNull(setCookie(response, "session_token")), "Secure")
    }

    @Test
    fun csrfCookieIsSecureByDefault() = withApp { client ->
        val response = client.get("/app/login")

        assertContains(assertNotNull(setCookie(response, CSRF_COOKIE)), "Secure")
    }

    @Test
    fun secureFlagCanBeDisabled() = withApp(AppConfig(secureCookies = false)) { client ->
        val response = client.get("/app/login")

        assertFalse(assertNotNull(setCookie(response, CSRF_COOKIE)).contains("Secure"))
    }
}
