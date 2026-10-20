package com.luisteixeira.familyplanner.auth.dto

import com.luisteixeira.familyplanner.auth.model.User
import com.luisteixeira.familyplanner.core.serialization.InstantSerializer
import com.luisteixeira.familyplanner.core.serialization.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class RegisterRequest(
    val email: String = "",
    val password: String = "",
    @SerialName("display_name") val displayName: String = "",
)

@Serializable
data class LoginRequest(
    val email: String = "",
    val password: String = "",
)

@Serializable
data class UserDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
)

@Serializable
data class AuthResponse(val user: UserDto, val token: String)

fun User.toDto(): UserDto = UserDto(id, email, displayName, createdAt)
