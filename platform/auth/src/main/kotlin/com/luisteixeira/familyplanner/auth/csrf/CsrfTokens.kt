package com.luisteixeira.familyplanner.auth.csrf

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class CsrfTokens(secret: ByteArray) {

    private val key = SecretKeySpec(secret, "HmacSHA256")
    private val random = SecureRandom()

    fun tokenFor(sessionToken: String): String {
        val mac = Mac.getInstance("HmacSHA256").apply { init(key) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(sessionToken.toByteArray()))
    }

    fun newToken(): String {
        val bytes = ByteArray(32).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun matches(expected: String, presented: String): Boolean =
        MessageDigest.isEqual(expected.toByteArray(), presented.toByteArray())
}
