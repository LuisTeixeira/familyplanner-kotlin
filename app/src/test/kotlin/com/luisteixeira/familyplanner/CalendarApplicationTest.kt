package com.luisteixeira.familyplanner

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun withApp(block: suspend (HttpClient) -> Unit) = testApplication {
    application { familyPlannerApp(composeApp()) }
    block(createClient { })
}

private suspend fun HttpClient.postJson(path: String, body: String, token: String? = null): HttpResponse =
    post(path) {
        contentType(ContentType.Application.Json)
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        setBody(body)
    }

private suspend fun HttpClient.putJson(path: String, body: String, token: String): HttpResponse =
    put(path) {
        contentType(ContentType.Application.Json)
        header(HttpHeaders.Authorization, "Bearer $token")
        setBody(body)
    }

private suspend fun HttpClient.getWithToken(path: String, token: String): HttpResponse =
    get(path) { header(HttpHeaders.Authorization, "Bearer $token") }

private suspend fun HttpClient.deleteWithToken(path: String, token: String): HttpResponse =
    delete(path) { header(HttpHeaders.Authorization, "Bearer $token") }

private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

private suspend fun HttpResponse.jsonList(): JsonArray = Json.parseToJsonElement(bodyAsText()).jsonArray

private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

private suspend fun HttpClient.registerUser(email: String): String =
    postJson(
        "/api/v1/register",
        "{\"email\":\"$email\",\"password\":\"secret123\",\"display_name\":\"User\"}",
    ).json().str("token")

private suspend fun HttpClient.createHousehold(name: String, token: String): String =
    postJson("/api/v1/households", "{\"name\":\"$name\"}", token).json().str("id")

private suspend fun HttpClient.createEventResponse(
    householdId: String,
    token: String,
    date: LocalDate,
    title: String,
    time: String? = null,
): HttpResponse {
    val timeField = time?.let { ",\"time\":\"$it\"" }.orEmpty()
    return postJson(
        "/api/v1/households/$householdId/events",
        "{\"title\":\"$title\",\"date\":\"$date\",\"timezone\":\"UTC\"$timeField}",
        token,
    )
}

private suspend fun HttpClient.createEvent(
    householdId: String,
    token: String,
    date: LocalDate,
    title: String,
    time: String? = null,
): JsonObject = createEventResponse(householdId, token, date, title, time).json()

class CalendarApplicationTest {

    private val today: LocalDate = LocalDate.now(ZoneOffset.UTC)
    private val monday: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    @Test
    fun createEventReturnsCreatedEvent() = withApp { client ->
        val token = client.registerUser("create@example.com")
        val householdId = client.createHousehold("Home", token)

        val response = client.createEventResponse(householdId, token, today, "Dentist")
        val body = response.json()

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("Dentist", body.str("title"))
        assertEquals("UTC", body.str("timezone"))
    }

    @Test
    fun createEventWithoutTitleIsRejected() = withApp { client ->
        val token = client.registerUser("notitle@example.com")
        val householdId = client.createHousehold("Home", token)

        val response = client.postJson(
            "/api/v1/households/$householdId/events",
            "{\"title\":\"\",\"date\":\"$today\"}",
            token,
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun listReturnsOnlyEventsInRange() = withApp { client ->
        val token = client.registerUser("list@example.com")
        val householdId = client.createHousehold("Home", token)
        client.createEvent(householdId, token, today.plusDays(2), "Inside")
        client.createEvent(householdId, token, today.plusDays(40), "Outside")

        val response = client.getWithToken(
            "/api/v1/households/$householdId/events?from=$today&to=${today.plusDays(7)}&timezone=UTC",
            token,
        )
        val events = response.jsonList()

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, events.size)
        assertEquals("Inside", events[0].jsonObject.str("title"))
    }

    @Test
    fun listRejectsRangeOverAYear() = withApp { client ->
        val token = client.registerUser("range@example.com")
        val householdId = client.createHousehold("Home", token)

        val response = client.getWithToken(
            "/api/v1/households/$householdId/events?from=2026-01-01&to=2028-01-01",
            token,
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun listRejectsUnknownTimezone() = withApp { client ->
        val token = client.registerUser("zone@example.com")
        val householdId = client.createHousehold("Home", token)

        val response = client.getWithToken(
            "/api/v1/households/$householdId/events?from=$today&to=$today&timezone=Mars/Base",
            token,
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun updateChangesTheEvent() = withApp { client ->
        val token = client.registerUser("update@example.com")
        val householdId = client.createHousehold("Home", token)
        val eventId = client.createEvent(householdId, token, today, "Dentist").str("id")

        val response = client.putJson(
            "/api/v1/households/$householdId/events/$eventId",
            "{\"title\":\"Doctor\",\"date\":\"$today\",\"timezone\":\"UTC\"}",
            token,
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Doctor", response.json().str("title"))
    }

    @Test
    fun deleteRemovesTheEvent() = withApp { client ->
        val token = client.registerUser("delete@example.com")
        val householdId = client.createHousehold("Home", token)
        val eventId = client.createEvent(householdId, token, today.plusDays(1), "Gone").str("id")

        val response = client.deleteWithToken("/api/v1/households/$householdId/events/$eventId", token)
        val remaining = client.getWithToken(
            "/api/v1/households/$householdId/events?from=$today&to=${today.plusDays(7)}&timezone=UTC",
            token,
        ).jsonList()

        assertTrue(response.status.isSuccess())
        assertTrue(remaining.isEmpty())
    }

    @Test
    fun anotherHouseholdCannotChangeOrDeleteAnEvent() = withApp { client ->
        val aliceToken = client.registerUser("alice-cal@example.com")
        val bobToken = client.registerUser("bob-cal@example.com")
        val aliceHousehold = client.createHousehold("Alice", aliceToken)
        val bobHousehold = client.createHousehold("Bob", bobToken)
        val aliceEvent = client.createEvent(aliceHousehold, aliceToken, today, "Private").str("id")

        val update = client.putJson(
            "/api/v1/households/$bobHousehold/events/$aliceEvent",
            "{\"title\":\"Hijack\",\"date\":\"$today\",\"timezone\":\"UTC\"}",
            bobToken,
        )
        val delete = client.deleteWithToken("/api/v1/households/$bobHousehold/events/$aliceEvent", bobToken)

        assertFalse(update.status.isSuccess())
        assertFalse(delete.status.isSuccess())
    }

    @Test
    fun weekPageRendersEventWithTime() = withApp { client ->
        val token = client.registerUser("week@example.com")
        val householdId = client.createHousehold("Home", token)
        client.createEvent(householdId, token, monday, "Standup", time = "10:30")

        val response = client.getWithToken(
            "/app/households/$householdId/calendar?week=$monday&timezone=UTC",
            token,
        )
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Standup"))
        assertTrue(html.contains("10:30"))
    }

    @Test
    fun householdDashboardRendersNextEvent() = withApp { client ->
        val token = client.registerUser("widget-event@example.com")
        val householdId = client.createHousehold("Home", token)
        client.createEvent(householdId, token, today.plusDays(2), "Dentist")

        val response = client.getWithToken("/app/households/$householdId", token)
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Dentist"))
    }

    @Test
    fun editFormIsPrefilledAndUsesPut() = withApp { client ->
        val token = client.registerUser("edit-form@example.com")
        val householdId = client.createHousehold("Home", token)
        val eventId = client.createEvent(householdId, token, today, "Dentist").str("id")

        val response = client.getWithToken("/app/households/$householdId/calendar/events/$eventId", token)
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("hx-put="))
        assertTrue(html.contains("Dentist"))
        assertTrue(html.contains("Delete"))
    }

    @Test
    fun newEventFormUsesPost() = withApp { client ->
        val token = client.registerUser("new-form@example.com")
        val householdId = client.createHousehold("Home", token)

        val response = client.getWithToken("/app/households/$householdId/calendar/events/new", token)
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("hx-post="))
        assertFalse(html.contains("Delete"))
    }
}
