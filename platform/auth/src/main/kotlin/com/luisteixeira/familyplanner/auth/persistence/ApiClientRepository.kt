package com.luisteixeira.familyplanner.auth.persistence

import com.luisteixeira.familyplanner.auth.model.ApiClient

interface ApiClientRepository {
    suspend fun saveClient(client: ApiClient)
    suspend fun findByKeyHash(keyHash: String): ApiClient?
}