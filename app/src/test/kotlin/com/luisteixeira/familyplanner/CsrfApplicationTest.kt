package com.luisteixeira.familyplanner

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private suspend fun HttpClient.registerUser(email: String) {
    post("/api/v1/register") {
        contentType(ContentType.Application.Json)
        setBody("{\"email\":\"$email\",\"password\":\"secret123\",\"display_name\":\"User\"}")
    }
}

class CsrfApplicationTest {

    @Test
    fun webSessionReceivesCsrfCookie() = withApp { client ->
        client.registerUser("csrf-get@example.com")
        val session = client.webSession(client.webLogin("csrf-get@example.com"))

        assertNotEquals("", session.csrfToken)
    }

    @Test
    fun unsafeRequestWithoutCsrfTokenIsForbidden() = withApp { client ->
        client.registerUser("csrf-missing@example.com")
        val sessionToken = client.webLogin("csrf-missing@example.com")

        val response = client.post("/app/households") {
            header(HttpHeaders.Cookie, "session_token=$sessionToken")
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("name=Home")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun unsafeRequestWithWrongCsrfTokenIsForbidden() = withApp { client ->
        client.registerUser("csrf-wrong@example.com")
        val session = client.webSession(client.webLogin("csrf-wrong@example.com"))

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
        client.registerUser("csrf-ok@example.com")
        val session = client.webSession(client.webLogin("csrf-ok@example.com"))

        val response = client.postForm(session, "/app/households", "name=Home")

        assertEquals(HttpStatusCode.SeeOther, response.status)
    }
}
