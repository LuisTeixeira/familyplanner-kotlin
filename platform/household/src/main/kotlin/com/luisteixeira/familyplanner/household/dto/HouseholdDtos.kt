package com.luisteixeira.familyplanner.household.dto

import com.luisteixeira.familyplanner.core.serialization.InstantSerializer
import com.luisteixeira.familyplanner.core.serialization.UUIDSerializer
import com.luisteixeira.familyplanner.household.model.Household
import com.luisteixeira.familyplanner.household.model.Invitation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class CreateHouseholdRequest(val name: String = "")

@Serializable
data class InviteRequest(val email: String = "")

@Serializable
data class HouseholdDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String,
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
)

@Serializable
data class InvitationDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("household_id")
    val householdId: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("invited_by")
    val invitedBy: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("invited_user_id")
    val invitedUserId: UUID,
    val status: String,
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
)

fun Household.toDto(): HouseholdDto = HouseholdDto(id, name, createdAt)

fun Invitation.toDto(): InvitationDto = InvitationDto(id, householdId, invitedBy, invitedUserId, status.name, createdAt)
