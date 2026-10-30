package com.luisteixeira.familyplanner.auth.session

import java.util.UUID

interface SessionValidator {
    suspend fun validateSession(token: String): UUID
}
