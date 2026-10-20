package com.luisteixeira.familyplanner.household.model

import java.time.Instant
import java.util.UUID

enum class HouseholdRole { OWNER, MEMBER }

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED }

data class Household(
    val id: UUID,
    val name: String,
    val createdAt: Instant,
)

data class HouseholdMember(
    val householdId: UUID,
    val userId: UUID,
    val role: HouseholdRole,
    val joinedAt: Instant,
)

data class Invitation(
    val id: UUID,
    val householdId: UUID,
    val invitedBy: UUID,
    val invitedUserId: UUID,
    val status: InvitationStatus,
    val createdAt: Instant,
)
