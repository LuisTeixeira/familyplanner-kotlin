package com.luisteixeira.familyplanner.auth.api

import com.luisteixeira.familyplanner.auth.users.UserDto
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(val user: UserDto, val token: String)
