package com.luisteixeira.familyplanner.household

import com.luisteixeira.familyplanner.auth.users.UserService
import com.luisteixeira.familyplanner.auth.users.InMemoryUserRepository
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.persistence.InMemoryTransactionRunner
import com.luisteixeira.familyplanner.household.persistence.InMemoryHouseholdRepository
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HouseholdServiceTest {

    private fun newServices(): Pair<UserService, HouseholdService> {
        val users = UserService(InMemoryUserRepository())
        val household = HouseholdService(InMemoryHouseholdRepository(), users, InMemoryTransactionRunner())
        return users to household
    }

    @Test
    fun invitedUserBecomesMemberAfterAccepting() = runBlocking<Unit> {
        val (users, households) = newServices()
        val owner = users.register("owner@example.com", "secret", "Owner").id
        val invitee = users.register("invitee@example.com", "secret", "Invitee").id
        val householdId = households.createHousehold(owner, "Family").id

        val invitation = households.inviteUser(householdId, owner, "invitee@example.com")
        assertFalse(households.isMember(householdId, invitee))

        households.acceptInvitation(invitation.id, invitee)

        assertTrue(households.isMember(householdId, invitee))
        assertEquals(emptyList(), households.listPendingInvitations(invitee))
    }

    @Test
    fun onlyTheInvitedUserCanAcceptOrDecline() = runBlocking<Unit> {
        val (users, households) = newServices()
        val owner = users.register("owner@example.com", "secret", "Owner").id
        users.register("invitee@example.com", "secret", "Invitee")
        users.register("stranger@example.com", "secret", "Stranger")
        val stranger = users.findUserIdByEmail("stranger@example.com")!!
        val householdId = households.createHousehold(owner, "Family").id
        val invitation = households.inviteUser(householdId, owner, "invitee@example.com")

        assertFailsWith<DomainException> { households.acceptInvitation(invitation.id, stranger) }
        assertFailsWith<DomainException> { households.declineInvitation(invitation.id, stranger) }
    }

    @Test
    fun nonMembersAreNotGrantedAccess() = runBlocking<Unit> {
        val (users, households) = newServices()
        val owner = users.register("owner@example.com", "secret", "Owner").id
        val householdId = households.createHousehold(owner, "Family").id

        assertFalse(households.isMember(householdId, UUID.randomUUID()))
    }
}
