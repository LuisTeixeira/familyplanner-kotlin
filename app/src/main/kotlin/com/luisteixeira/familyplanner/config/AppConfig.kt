package com.luisteixeira.familyplanner.config

enum class Persistence { MEMORY, POSTGRES }

class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
) {
    override fun toString(): String {
        return "DatabaseConfig(url=$url, user=$user, password=***)"
    }
}

data class AppConfig(
    val persistence: Persistence = Persistence.MEMORY,
    val database: DatabaseConfig? = null,
) {

    init {
        require(persistence == Persistence.MEMORY || database != null) {
            "database settings are required when persistence is POSTGRES"
        }
    }

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): AppConfig {
            val persistence = env["FAMILYPLANNER_PERSISTENCE"]?.let { Persistence.valueOf(it.uppercase()) } ?: Persistence.MEMORY

            val database = if (persistence == Persistence.POSTGRES) {
                DatabaseConfig(
                    url = env.required("FAMILYPLANNER_DB_URL"),
                    user = env.required("FAMILYPLANNER_DB_USER"),
                    password = env.required("FAMILYPLANNER_DB_PASSWORD"),
                )
            } else {
                null
            }
            return AppConfig(persistence, database)
        }
    }

}

private fun Map<String, String>.required(key: String): String {
    return this[key] ?: error("$key must be set when FAMILYPLANNER_PERSISTENCE is postgres")
}