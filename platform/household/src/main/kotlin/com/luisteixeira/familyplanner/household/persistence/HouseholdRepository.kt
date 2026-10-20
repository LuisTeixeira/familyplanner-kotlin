package com.luisteixeira.familyplanner.household.persistence

import com.luisteixeira.familyplanner.household.model.Household
import com.luisteixeira.familyplanner.household.model.HouseholdMember
import com.luisteixeira.familyplanner.household.model.Invitation
import java.util.UUID

interface HouseholdRepository {
    suspend fun saveHousehold(household: Household)
    suspend fun findHousehold(householdId: UUID): Household?
    suspend fun saveMember(member: HouseholdMember)
    suspend fun isMember(householdId: UUID, userId: UUID): Boolean
    suspend fun listHouseholdsForUser(userId: UUID): List<Household>
    suspend fun saveInvitation(invitation: Invitation)
    suspend fun findInvitation(invitationId: UUID): Invitation?
    suspend fun listPendingInvitations(userId: UUID): List<Invitation>
    suspend fun findPendingInvitation(householdId: UUID, userId: UUID): Invitation?
}
