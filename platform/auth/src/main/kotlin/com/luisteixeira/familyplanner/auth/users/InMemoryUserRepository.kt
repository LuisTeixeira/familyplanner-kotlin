package com.luisteixeira.familyplanner.auth.users

import com.luisteixeira.familyplanner.core.persistence.DuplicateEntryException

class InMemoryUserRepository : UserRepository {
    private val lock = Any()
    private val usersByEmail = HashMap<String, User>()

    override suspend fun saveUser(user: User) {
        synchronized(lock) {
            if (usersByEmail.containsKey(user.email)) throw DuplicateEntryException("user already exists")
            usersByEmail[user.email] = user
        }
    }

    override suspend fun findUserByEmail(email: String): User? = synchronized(lock) { usersByEmail[email] }
}
