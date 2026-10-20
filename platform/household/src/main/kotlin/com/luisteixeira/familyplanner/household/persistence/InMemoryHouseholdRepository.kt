package com.luisteixeira.familyplanner.household.persistence

import com.luisteixeira.familyplanner.household.model.Household
import com.luisteixeira.familyplanner.household.model.HouseholdMember
import com.luisteixeira.familyplanner.household.model.Invitation
import com.luisteixeira.familyplanner.household.model.InvitationStatus
import java.util.UUID

class InMemoryHouseholdRepository : HouseholdRepository {
    private val lock = Any()
    private val households = HashMap<UUID, Household>()
    private val members = HashMap<UUID, HashMap<UUID, HouseholdMember>>()
    private val invitations = HashMap<UUID, Invitation>()

    override suspend fun saveHousehold(household: Household) {
        synchronized(lock) { households[household.id] = household }
    }

    override suspend fun findHousehold(householdId: UUID): Household? =
        synchronized(lock) { households[householdId] }

    override suspend fun saveMember(member: HouseholdMember) {
        synchronized(lock) { members.getOrPut(member.householdId) { HashMap() }[member.userId] = member }
    }

    override suspend fun isMember(householdId: UUID, userId: UUID): Boolean =
        synchronized(lock) { members[householdId]?.containsKey(userId) == true }

    override suspend fun listHouseholdsForUser(userId: UUID): List<Household> = synchronized(lock) {
        members.filterValues { it.containsKey(userId) }.keys.mapNotNull { households[it] }
    }

    override suspend fun saveInvitation(invitation: Invitation) {
        synchronized(lock) { invitations[invitation.id] = invitation }
    }

    override suspend fun findInvitation(invitationId: UUID): Invitation? =
        synchronized(lock) { invitations[invitationId] }

    override suspend fun listPendingInvitations(userId: UUID): List<Invitation> = synchronized(lock) {
        invitations.values.filter { it.invitedUserId == userId && it.status == InvitationStatus.PENDING }
    }

    override suspend fun findPendingInvitation(householdId: UUID, userId: UUID): Invitation? = synchronized(lock) {
        invitations.values.firstOrNull {
            it.householdId == householdId && it.invitedUserId == userId && it.status == InvitationStatus.PENDING
        }
    }
}
