package com.luisteixeira.familyplanner.household

import com.luisteixeira.familyplanner.auth.AuthService
import com.luisteixeira.familyplanner.auth.persistence.InMemoryAuthRepository
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

    private fun newServices(): Pair<AuthService, HouseholdService> {
        val auth = AuthService(InMemoryAuthRepository())
        val household = HouseholdService(InMemoryHouseholdRepository(), auth, InMemoryTransactionRunner())
        return auth to household
    }

    @Test
    fun invitedUserBecomesMemberAfterAccepting() = runBlocking<Unit> {
        val (auth, households) = newServices()
        val owner = auth.register("owner@example.com", "secret", "Owner").user.id
        val invitee = auth.register("invitee@example.com", "secret", "Invitee").user.id
        val householdId = households.createHousehold(owner, "Family").id

        val invitation = households.inviteUser(householdId, owner, "invitee@example.com")
        assertFalse(households.isMember(householdId, invitee))

        households.acceptInvitation(invitation.id, invitee)

        assertTrue(households.isMember(householdId, invitee))
        assertEquals(emptyList(), households.listPendingInvitations(invitee))
    }

    @Test
    fun onlyTheInvitedUserCanAcceptOrDecline() = runBlocking<Unit> {
        val (auth, households) = newServices()
        val owner = auth.register("owner@example.com", "secret", "Owner").user.id
        auth.register("invitee@example.com", "secret", "Invitee")
        auth.register("stranger@example.com", "secret", "Stranger")
        val stranger = auth.findUserIdByEmail("stranger@example.com")!!
        val householdId = households.createHousehold(owner, "Family").id
        val invitation = households.inviteUser(householdId, owner, "invitee@example.com")

        assertFailsWith<DomainException> { households.acceptInvitation(invitation.id, stranger) }
        assertFailsWith<DomainException> { households.declineInvitation(invitation.id, stranger) }
    }

    @Test
    fun nonMembersAreNotGrantedAccess() = runBlocking<Unit> {
        val (auth, households) = newServices()
        val owner = auth.register("owner@example.com", "secret", "Owner").user.id
        val householdId = households.createHousehold(owner, "Family").id

        assertFalse(households.isMember(householdId, UUID.randomUUID()))
    }
}
