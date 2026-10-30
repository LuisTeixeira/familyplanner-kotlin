package com.luisteixeira.familyplanner.auth.session

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

fun ApplicationCall.setSessionCookie(session: Session, secure: Boolean = true) {
    val attributes = listOfNotNull(
        "$SESSION_COOKIE=${session.token}",
        "Path=/",
        "Expires=${httpDate.format(session.expiresAt)}",
        "HttpOnly",
        "SameSite=Lax",
        if (secure) "Secure" else null,
    )

    response.headers.append(
        HttpHeaders.SetCookie,
        attributes.joinToString("; ")
    )
}

fun ApplicationCall.clearSessionCookie(secure: Boolean = true) {
    val attributes = listOfNotNull(
        "$SESSION_COOKIE=",
        "Path=/",
        "Max-Age=0",
        "HttpOnly",
        if (secure) "Secure" else null,
    )
    response.headers.append(
        HttpHeaders.SetCookie,
        attributes.joinToString("; ")
    )
}
