package com.luisteixeira.familyplanner.auth.apikey


class InMemoryApiClientRepository : ApiClientRepository {
    private val lock = Any()
    private val clients = HashMap<String, ApiClient>()

    override suspend fun saveClient(client: ApiClient) {
        synchronized(lock) {
            clients[client.keyHash] = client
        }
    }

    override suspend fun findByKeyHash(keyHash: String): ApiClient? {
        return synchronized(lock) {
            clients[keyHash]
        }
    }
}
