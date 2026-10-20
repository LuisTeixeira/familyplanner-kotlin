package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.Session
import com.luisteixeira.familyplanner.auth.model.User

interface AuthRepository {
    suspend fun saveUser(user: User)
    suspend fun findUserByEmail(email: String): User?
    suspend fun saveSession(session: Session)
    suspend fun findSession(token: String): Session?
    suspend fun deleteSession(token: String)
}
