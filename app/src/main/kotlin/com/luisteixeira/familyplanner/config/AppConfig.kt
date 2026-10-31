package com.luisteixeira.familyplanner.config

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
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

class JwtConfig(
    val keys: KeyPair,
    val issuer: String = "familyplanner",
    val audience: String = "familyplanner-api"
)

data class AppConfig(
    val persistence: Persistence = Persistence.MEMORY,
    val database: DatabaseConfig? = null,
    val secret: Secret = Secret(randomSecret()),
    val secureCookies: Boolean = true,
    val jwt: JwtConfig = JwtConfig(generateRsaKeyPair()),
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

            val jwt = JwtConfig(
                keys = env["FAMILYPLANNER_JWT_PRIVATE_KEY"]?.let { loadPrivateKey(Path.of(it)) }
                    ?: if (persistence == Persistence.POSTGRES) {
                        error("FAMILYPLANNER_JWT_PRIVATE_KEY must be set when FAMILYPLANNER_PERSISTENCE is postgres")
                    } else {
                        generateRsaKeyPair()
                    },
                issuer = env["FAMILYPLANNER_JWT_ISSUER"] ?: "familyplanner",
                audience = env["FAMILYPLANNER_JWT_AUDIENCE"] ?: "familyplanner-api",
            )

            return AppConfig(persistence, database, secret, secureCookies, jwt)
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

private fun generateRsaKeyPair(): KeyPair =
    KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

private fun loadPrivateKey(path: Path): KeyPair {
    require(Files.isRegularFile(path)) {
        "FAMILYPLANNER_JWT_PRIVATE_KEY must point to an existing file"
    }
    val pem = Files.readString(path)
    require("BEGIN PRIVATE KEY" in pem) {
        "FAMILYPLANNER_JWT_PRIVATE_KEY must be a PKCS#8 PEM file (BEGIN PRIVATE KEY)"
    }
    val body = pem.lineSequence()
        .filterNot { it.startsWith("-----") }
        .joinToString("")
    val privateKey = KeyFactory.getInstance("RSA")
        .generatePrivate(PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(body))) as RSAPrivateCrtKey
    val publicKey = KeyFactory.getInstance("RSA")
        .generatePublic(RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent))
    return KeyPair(publicKey, privateKey)
}