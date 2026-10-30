package com.luisteixeira.familyplanner.auth.apikey


interface ApiClientRepository {
    suspend fun saveClient(client: ApiClient)
    suspend fun findByKeyHash(keyHash: String): ApiClient?
}
