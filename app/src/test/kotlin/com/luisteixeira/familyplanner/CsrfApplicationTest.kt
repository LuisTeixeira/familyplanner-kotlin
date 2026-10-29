package com.luisteixeira.familyplanner

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private fun withApp(block: suspend (HttpClient) -> Unit) = testApplication {
    application { familyPlannerApp(composeApp()) }
    block(createClient { })
}

private suspend fun HttpClient.registerUser(email: String): String {
    val body = post("/api/v1/register") {
        contentType(ContentType.Application.Json)
        setBody("{\"email\":\"$email\",\"password\":\"secret123\",\"display_name\":\"User\"}")
    }.bodyAsText()
    return Json.parseToJsonElement(body).jsonObject.getValue("token").jsonPrimitive.content
}

class CsrfApplicationTest {

    @Test
    fun webSessionReceivesCsrfCookie() = withApp { client ->
        val session = client.webSession(client.registerUser("csrf-get@example.com"))

        assertNotEquals("", session.csrfToken)
    }

    @Test
    fun unsafeRequestWithoutCsrfTokenIsForbidden() = withApp { client ->
        val token = client.registerUser("csrf-missing@example.com")

        val response = client.post("/app/households") {
            header(HttpHeaders.Cookie, "session_token=$token")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("name=Home")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun unsafeRequestWithWrongCsrfTokenIsForbidden() = withApp { client ->
        val session = client.webSession(client.registerUser("csrf-wrong@example.com"))

        val response = client.post("/app/households") {
            header(HttpHeaders.Cookie, session.cookies)
            header("X-CSRF-Token", "not-the-token")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("name=Home")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun unsafeRequestWithMatchingCsrfTokenIsAccepted() = withApp { client ->
        val session = client.webSession(client.registerUser("csrf-ok@example.com"))

        val response = client.postForm(session, "/app/households", "name=Home")

        assertEquals(HttpStatusCode.SeeOther, response.status)
    }
}