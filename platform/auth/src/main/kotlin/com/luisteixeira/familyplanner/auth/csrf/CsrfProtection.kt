package com.luisteixeira.familyplanner.auth.csrf

import com.luisteixeira.familyplanner.auth.SESSION_COOKIE
import com.luisteixeira.familyplanner.core.web.HttpError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.httpMethod

const val CSRF_COOKIE = "csrf_token"
const val CSRF_HEADER = "X-CSRF-Token"

class CsrfConfig {
    lateinit var tokens: CsrfTokens
    var secureCookies: Boolean = true
}

private val safeMethods = setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)

val CsrfProtection = createRouteScopedPlugin("CsrfProtection", ::CsrfConfig) {
    val tokens = pluginConfig.tokens
    val secure = pluginConfig.secureCookies

    onCall { call ->
        val sessionToken = call.request.cookies[SESSION_COOKIE]
        val isSafe = call.request.httpMethod in safeMethods

        if (sessionToken != null) {
            checkSessionBound(call, tokens, secure, sessionToken, isSafe)
        } else {
            checkAnonymous(call, tokens, secure, isSafe)
        }
    }
}

private fun checkSessionBound(
    call: ApplicationCall,
    tokens: CsrfTokens,
    secure: Boolean,
    sessionToken: String,
    isSafe: Boolean,
) {
    val expected = tokens.tokenFor(sessionToken)
    if (isSafe) {
        setCsrfCookie(call, expected, secure)
    } else if (!isValidSubmission(call, tokens, expected)) {
        throw HttpError(HttpStatusCode.Forbidden, "Forbidden: invalid CSRF token")
    }
}

private fun checkAnonymous(
    call: ApplicationCall,
    tokens: CsrfTokens,
    secure: Boolean,
    isSafe: Boolean,
) {
    val cookie = call.request.cookies[CSRF_COOKIE]
    if (isSafe) {
        if (cookie == null) setCsrfCookie(call, tokens.newToken(), secure)
    } else if (cookie == null || !isValidSubmission(call, tokens, cookie)) {
        throw HttpError(HttpStatusCode.Forbidden, "Forbidden: invalid CSRF token")
    }
}

private fun isValidSubmission(call: ApplicationCall, tokens: CsrfTokens, expected: String): Boolean {
    val cookie = call.request.cookies[CSRF_COOKIE] ?: return false
    val header = call.request.headers[CSRF_HEADER] ?: return false
    return tokens.matches(expected, cookie) && tokens.matches(expected, header)
}

private fun setCsrfCookie(call: ApplicationCall, value: String, secure: Boolean) {
    val attributes = listOfNotNull(
        "$CSRF_COOKIE=$value",
        "Path=/",
        "SameSite=Strict",
        if (secure) "Secure" else null,
    )
    call.response.headers.append(HttpHeaders.SetCookie, attributes.joinToString("; "))
}
