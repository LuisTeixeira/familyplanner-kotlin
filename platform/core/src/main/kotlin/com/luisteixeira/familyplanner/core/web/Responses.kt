package com.luisteixeira.familyplanner.core.web

import com.luisteixeira.familyplanner.core.domain.DomainException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpError(val status: HttpStatusCode, message: String) : RuntimeException(message)

@Serializable
data class MessageResponse(val message: String)

inline fun <T> httpCatch(status: HttpStatusCode, block: () -> T): T =
    try {
        block()
    } catch (e: DomainException) {
        throw HttpError(status, e.message.orEmpty())
    }

val AppJson = Json { ignoreUnknownKeys = true }

suspend inline fun <reified T> ApplicationCall.receiveJsonOr400(message: String): T =
    try {
        AppJson.decodeFromString<T>(receiveText())
    } catch (e: SerializationException) {
        throw HttpError(HttpStatusCode.BadRequest, message)
    }

suspend fun ApplicationCall.respondHtml(html: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respondText(html, ContentType.Text.Html.withCharset(Charsets.UTF_8), status)

suspend inline fun <reified T> ApplicationCall.respondJson(value: T, status: HttpStatusCode = HttpStatusCode.OK) =
    respondText(AppJson.encodeToString(value), ContentType.Application.Json, status)

suspend fun ApplicationCall.redirectSeeOther(location: String) {
    response.header(HttpHeaders.Location, location)
    respond(HttpStatusCode.SeeOther)
}

suspend fun ApplicationCall.hxRedirect(location: String) {
    response.header("HX-Redirect", location)
    respond(HttpStatusCode.OK)
}

fun ApplicationCall.isHtmx(): Boolean = request.headers["HX-Request"] == "true"

fun String.escapeHtml(): String = buildString(length) {
    for (c in this@escapeHtml) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }
}
