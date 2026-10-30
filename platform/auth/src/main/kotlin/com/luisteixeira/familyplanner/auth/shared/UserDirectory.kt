package com.luisteixeira.familyplanner.auth.shared

import java.util.UUID

interface UserDirectory {
    suspend fun findUserIdByEmail(email: String): UUID?
}
