package com.luisteixeira.familyplanner.config

import java.security.SecureRandom
import java.util.Base64

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

class Secret(val value: String) {
    init {
        require(value.length >= 32) {
            "FAMILYPLANNER_SECRET must be at least 32 characters"
        }
    }

    override fun toString(): String {
        return "Secret(***)"
    }
}

data class AppConfig(
    val persistence: Persistence = Persistence.MEMORY,
    val database: DatabaseConfig? = null,
    val secret: Secret = Secret(randomSecret()),
    val secureCookies: Boolean = true,
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

            val secret = env["FAMILYPLANNER_SECRET"]?.let { Secret(it) }
                ?: if (persistence == Persistence.POSTGRES) {
                    error("FAMILYPLANNER_SECRET must be set when FAMILYPLANNER_PERSISTENCE is postgres")
                } else {
                    Secret(randomSecret())
                }

            val secureCookies = env["FAMILYPLANNER_SECURE_COOKIES"]
                ?.let { it.toBooleanStrictOrNull() ?: error("FAMILYPLANNER_SECURE_COOKIES must be true or false")}
                ?: true

            return AppConfig(persistence, database, secret, secureCookies)
        }
    }

}

private fun Map<String, String>.required(key: String): String {
    return this[key] ?: error("$key must be set when FAMILYPLANNER_PERSISTENCE is postgres")
}

private fun randomSecret(): String {
    val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
    return Base64.getEncoder().encodeToString(bytes)
}