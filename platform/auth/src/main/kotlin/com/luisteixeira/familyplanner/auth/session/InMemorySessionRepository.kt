package com.luisteixeira.familyplanner.auth.session

class InMemorySessionRepository : SessionRepository {
    private val lock = Any()
    private val sessions = HashMap<String, Session>()

    override suspend fun saveSession(session: Session) {
        synchronized(lock) { sessions[session.token] = session }
    }

    override suspend fun findSession(token: String): Session? = synchronized(lock) { sessions[token] }

    override suspend fun deleteSession(token: String) {
        synchronized(lock) { sessions.remove(token) }
    }
}
