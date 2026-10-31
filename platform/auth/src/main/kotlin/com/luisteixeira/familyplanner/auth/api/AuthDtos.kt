package com.luisteixeira.familyplanner.auth.api

import com.luisteixeira.familyplanner.auth.users.UserDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val user: UserDto,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String = "",
)