package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.Session
import com.luisteixeira.familyplanner.auth.model.User
import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException

class InMemoryAuthRepository : AuthRepository {
    private val lock = Any()
    private val usersByEmail = HashMap<String, User>()
    private val sessions = HashMap<String, Session>()

    override suspend fun saveUser(user: User) {
        synchronized(lock) {
            if (usersByEmail.containsKey(user.email)) throw DuplicateEntryException("user already exists")
            usersByEmail[user.email] = user
        }
    }

    override suspend fun findUserByEmail(email: String): User? = synchronized(lock) { usersByEmail[email] }

    override suspend fun saveSession(session: Session) {
        synchronized(lock) { sessions[session.token] = session }
    }

    override suspend fun findSession(token: String): Session? = synchronized(lock) { sessions[token] }

    override suspend fun deleteSession(token: String) {
        synchronized(lock) { sessions.remove(token) }
    }
}
