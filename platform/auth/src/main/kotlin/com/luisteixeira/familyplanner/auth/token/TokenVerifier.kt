package com.luisteixeira.familyplanner.auth.token

import java.util.UUID

interface TokenVerifier {
    fun verify(token: String): UUID?
}