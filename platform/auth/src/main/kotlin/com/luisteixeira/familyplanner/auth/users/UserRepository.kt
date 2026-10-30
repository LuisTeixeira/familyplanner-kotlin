package com.luisteixeira.familyplanner.auth.users

interface UserRepository {
    suspend fun saveUser(user: User)
    suspend fun findUserByEmail(email: String): User?
}
