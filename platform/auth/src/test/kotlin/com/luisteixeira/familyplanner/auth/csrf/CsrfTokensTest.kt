package com.luisteixeira.familyplanner.auth.csrf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CsrfTokensTest {

    private val tokens = CsrfTokens("test-secret-that-is-long-enough-123456".toByteArray())

    @Test
    fun tokenIsStableForTheSameSession() {
        assertEquals(tokens.tokenFor("abc"), tokens.tokenFor("abc"))
    }

    @Test
    fun tokenDiffersBetweenSessions() {
        assertNotEquals(tokens.tokenFor("abc"), tokens.tokenFor("xyz"))
    }

    @Test
    fun tokenDiffersAnotherSecret() {
        val other = CsrfTokens("another-secret-that-is-long-enough-99".toByteArray())

        assertNotEquals(tokens.tokenFor("abc"), other.tokenFor("abc"))
    }

    @Test
    fun matchesOnlyTheExactToken() {
        val token = tokens.tokenFor("abc")

        assertTrue(tokens.matches(token, token))
        assertFalse(tokens.matches(token, tokens.tokenFor("xyz")))
    }

}