package com.luisteixeira.familyplanner.auth.session

interface SessionRepository {
    suspend fun saveSession(session: Session)
    suspend fun findSession(token: String): Session?
    suspend fun deleteSession(token: String)
}
