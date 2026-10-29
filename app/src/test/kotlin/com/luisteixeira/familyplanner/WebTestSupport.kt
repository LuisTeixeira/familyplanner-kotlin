package com.luisteixeira.familyplanner

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class WebSession(
    val sessionToken: String,
    val csrfToken: String,
) {
    val cookies: String get() = "session_token=$sessionToken;csrf_token=$csrfToken"
}

suspend fun HttpClient.webSession(sessionToken: String, path: String = "/app/households"): WebSession {
    val response = get(path) { header(HttpHeaders.Cookie, "session_token=$sessionToken") }
    val csrf = response.headers.getAll(HttpHeaders.SetCookie)
        ?.firstNotNullOfOrNull { Regex("csrf_token=([^;]+)").find(it)?.groupValues?.get(1) }
        ?: error("no csrf_token cookie returns for path $path")
    return WebSession(sessionToken, csrf)
}

suspend fun HttpClient.postForm(session: WebSession, path: String, form: String): HttpResponse =
    post(path) {
        header(HttpHeaders.Cookie, session.cookies)
        header("X-CSRF-Token", session.csrfToken)
        contentType(ContentType.Application.FormUrlEncoded)
        setBody(form)
    }