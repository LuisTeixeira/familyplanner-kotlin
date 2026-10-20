package com.luisteixeira.familyplanner.auth

import com.luisteixeira.familyplanner.auth.model.Session
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC)

fun ApplicationCall.setSessionCookie(session: Session) {
    response.headers.append(
        HttpHeaders.SetCookie,
        "$SESSION_COOKIE=${session.token}; Path=/; Expires=${httpDate.format(session.expiresAt)}; HttpOnly; SameSite=Lax",
    )
}

fun ApplicationCall.clearSessionCookie() {
    response.headers.append(HttpHeaders.SetCookie, "$SESSION_COOKIE=; Path=/; Max-Age=0; HttpOnly")
}
