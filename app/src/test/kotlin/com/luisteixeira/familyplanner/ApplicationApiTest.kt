package com.luisteixeira.familyplanner

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private suspend fun HttpClient.postJson(path: String, body: String, token: String? = null): HttpResponse =
    post(path) {
        contentType(ContentType.Application.Json)
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        setBody(body)
    }

private suspend fun HttpClient.patchWithToken(path: String, token: String): HttpResponse =
    patch(path) { header(HttpHeaders.Authorization, "Bearer $token") }

private suspend fun HttpClient.getWithToken(path: String, token: String): HttpResponse =
    get(path) { header(HttpHeaders.Authorization, "Bearer $token") }

private suspend fun HttpResponse.json(): JsonObject = Json.parseToJsonElement(bodyAsText()).jsonObject

private suspend fun HttpResponse.jsonList(): JsonArray = Json.parseToJsonElement(bodyAsText()).jsonArray

private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

private suspend fun HttpClient.registerUser(email: String, displayName: String): Pair<String, String> {
    val body = postJson(
        "/api/v1/register",
        """{"email":"$email","password":"secret123","display_name":"$displayName"}""",
    ).json()
    return body.getValue("user").jsonObject.str("id") to body.str("access_token")
}

private suspend fun HttpClient.createHousehold(name: String, token: String): String =
    postJson("/api/v1/households", """{"name":"$name"}""", token).json().str("id")

private suspend fun HttpClient.createList(householdId: String, name: String, token: String): String =
    postJson("/api/v1/households/$householdId/shopping/lists", """{"name":"$name"}""", token).json().str("id")

private suspend fun HttpClient.addItem(householdId: String, listId: String, name: String, token: String): String =
    postJson(
        "/api/v1/households/$householdId/shopping/lists/$listId/items",
        """{"name":"$name"}""",
        token,
    ).json().str("id")

class ApplicationApiTest {

    @Test
    fun registerReturnsUserWithoutPasswordHash() = withApp { client ->
        val response = client.postJson(
            "/api/v1/register",
            """{"email":"ann@example.com","password":"secret123","display_name":"Ann"}""",
        )
        val body = response.json()
        val user = body.getValue("user").jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ann@example.com", user.str("email"))
        assertEquals("Ann", user.str("display_name"))
        assertNull(user["password_hash"])
        assertTrue(body.str("access_token").isNotEmpty())
    }

    @Test
    fun duplicateRegistrationIsRejected() = withApp { client ->
        client.registerUser("dup@example.com", "Dup")

        val response = client.postJson(
            "/api/v1/register",
            """{"email":"dup@example.com","password":"secret123","display_name":"Dup"}""",
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun wrongPasswordIsUnauthorized() = withApp { client ->
        client.registerUser("login@example.com", "Login")

        val response = client.postJson(
            "/api/v1/login",
            """{"email":"login@example.com","password":"wrong"}""",
        )

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun protectedEndpointRequiresAuthentication() = withApp { client ->
        val response = client.get("/api/v1/households")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun createdHouseholdAppearsInUserList() = withApp { client ->
        val (_, token) = client.registerUser("owner@example.com", "Owner")

        val householdId = client.createHousehold("Teixeira", token)
        val households = client.getWithToken("/api/v1/households", token).jsonList()

        assertEquals(1, households.size)
        assertEquals(householdId, households[0].jsonObject.str("id"))
        assertEquals("Teixeira", households[0].jsonObject.str("name"))
    }

    @Test
    fun invitationCanBeSentAndAccepted() = withApp { client ->
        val (_, ownerToken) = client.registerUser("owner2@example.com", "Owner")
        val (_, inviteeToken) = client.registerUser("invitee@example.com", "Invitee")
        val householdId = client.createHousehold("Family", ownerToken)

        val invitation = client.postJson(
            "/api/v1/households/$householdId/invites",
            """{"email":"invitee@example.com"}""",
            ownerToken,
        ).json()
        val accept = client.postJson("/api/v1/invitations/${invitation.str("id")}/accept", "", inviteeToken)
        val inviteeHouseholds = client.getWithToken("/api/v1/households", inviteeToken).jsonList()

        assertEquals(HttpStatusCode.OK, accept.status)
        assertEquals(householdId, inviteeHouseholds.single().jsonObject.str("id"))
    }

    @Test
    fun shoppingListItemFlowWorksForMember() = withApp { client ->
        val (_, token) = client.registerUser("shopper@example.com", "Shopper")
        val householdId = client.createHousehold("Home", token)
        val listId = client.createList(householdId, "Groceries", token)
        val itemId = client.addItem(householdId, listId, "Milk", token)

        val detail = client.getWithToken("/api/v1/households/$householdId/shopping/lists/$listId", token).json()
        val toggled = client.patchWithToken("/api/v1/households/$householdId/shopping/items/$itemId/toggle", token).json()

        assertEquals("Groceries", detail.str("name"))
        assertEquals(1, detail.getValue("items").jsonArray.size)
        assertTrue(toggled.getValue("is_completed").jsonPrimitive.content.toBoolean())
    }

    @Test
    fun itemCannotBeAddedToAnotherHouseholdsList() = withApp { client ->
        val (_, aliceToken) = client.registerUser("alice@example.com", "Alice")
        val (_, bobToken) = client.registerUser("bob@example.com", "Bob")
        val aliceHousehold = client.createHousehold("Alice", aliceToken)
        val bobHousehold = client.createHousehold("Bob", bobToken)
        val aliceList = client.createList(aliceHousehold, "Private", aliceToken)

        val response = client.postJson(
            "/api/v1/households/$bobHousehold/shopping/lists/$aliceList/items",
            """{"name":"Intruder"}""",
            bobToken,
        )

        assertFalse(response.status.isSuccess())
    }

    @Test
    fun itemCannotBeToggledByAnotherHousehold() = withApp { client ->
        val (_, aliceToken) = client.registerUser("alice2@example.com", "Alice")
        val (_, bobToken) = client.registerUser("bob2@example.com", "Bob")
        val aliceHousehold = client.createHousehold("Alice", aliceToken)
        val bobHousehold = client.createHousehold("Bob", bobToken)
        val aliceList = client.createList(aliceHousehold, "Private", aliceToken)
        val aliceItem = client.addItem(aliceHousehold, aliceList, "Secret", aliceToken)

        val response = client.patchWithToken(
            "/api/v1/households/$bobHousehold/shopping/items/$aliceItem/toggle",
            bobToken,
        )

        assertFalse(response.status.isSuccess())
    }

    @Test
    fun itemCannotBeToggledThroughWrongHouseholdInUrl() = withApp { client ->
        val (_, aliceToken) = client.registerUser("alice3@example.com", "Alice")
        val (_, bobToken) = client.registerUser("bob3@example.com", "Bob")
        val aliceHousehold = client.createHousehold("Alice", aliceToken)
        val bobHousehold = client.createHousehold("Bob", bobToken)
        val aliceList = client.createList(aliceHousehold, "Private", aliceToken)
        val aliceItem = client.addItem(aliceHousehold, aliceList, "Secret", aliceToken)

        val response = client.patchWithToken(
            "/api/v1/households/$aliceHousehold/shopping/items/$aliceItem/toggle",
            bobToken,
        )

        assertFalse(response.status.isSuccess())
    }

    @Test
    fun loginPageReferencesModuleOwnedAssets() = withApp { client ->
        val html = client.get("/app/login").bodyAsText()

        assertTrue(html.contains("/static/platform/vendor/htmx.min.js"))
        assertTrue(html.contains("/static/auth/css/auth.css"))
    }

    @Test
    fun staticAssetsAreServedFromTheirOwningModule() = withApp { client ->
        assertEquals(HttpStatusCode.OK, client.get("/static/platform/vendor/htmx.min.js").status)
        assertEquals(HttpStatusCode.OK, client.get("/static/platform/css/common.css").status)
        assertEquals(HttpStatusCode.OK, client.get("/static/auth/css/auth.css").status)
        assertEquals(HttpStatusCode.OK, client.get("/static/household/css/household.css").status)
        assertEquals(HttpStatusCode.OK, client.get("/static/shopping/css/shopping_dashboard.css").status)
    }

    @Test
    fun householdDashboardRendersShoppingWidget() = withApp { client ->
        val (_, token) = client.registerUser("widget@example.com", "Widget")
        val householdId = client.createHousehold("Dash", token)
        val sessionToken = client.webLogin("widget@example.com")

        val response = client.get("/app/households/$householdId") {
            header(HttpHeaders.Cookie, "session_token=$sessionToken")
        }
        val html = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(html.contains("Shopping Lists"))
        assertTrue(html.contains("/app/households/$householdId/shopping/lists"))
    }
}
