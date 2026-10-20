package com.luisteixeira.familyplanner.household

import com.luisteixeira.familyplanner.auth.UserDirectory
import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.persistence.TransactionRunner
import com.luisteixeira.familyplanner.household.model.Household
import com.luisteixeira.familyplanner.household.model.HouseholdMember
import com.luisteixeira.familyplanner.household.model.HouseholdRole
import com.luisteixeira.familyplanner.household.model.Invitation
import com.luisteixeira.familyplanner.household.model.InvitationStatus
import com.luisteixeira.familyplanner.household.persistence.HouseholdRepository
import java.time.Clock
import java.util.UUID

class HouseholdService(
    private val repo: HouseholdRepository,
    private val users: UserDirectory,
    private val transactions: TransactionRunner,
    private val clock: Clock = Clock.systemUTC(),
) : HouseholdAccess {

    suspend fun createHousehold(userId: UUID, name: String): Household {
        if (name.isEmpty()) throw DomainException("household name is required")

        val now = clock.instant()
        val household = Household(UUID.randomUUID(), name, now)
        transactions.inTransaction {
            repo.saveHousehold(household)
            repo.saveMember(HouseholdMember(household.id, userId, HouseholdRole.OWNER, now))
        }
        return household
    }

    suspend fun getHousehold(householdId: UUID): Household =
        repo.findHousehold(householdId) ?: throw DomainException("Household doesn't exist")

    override suspend fun isMember(householdId: UUID, userId: UUID): Boolean =
        repo.isMember(householdId, userId)

    suspend fun listUserHouseholds(userId: UUID): List<Household> =
        repo.listHouseholdsForUser(userId)

    suspend fun inviteUser(householdId: UUID, invitedBy: UUID, email: String): Invitation {
        getHousehold(householdId)

        val invitedUserId = users.findUserIdByEmail(email)
            ?: throw DomainException("user with email $email not found")
        if (repo.isMember(householdId, invitedUserId)) {
            throw DomainException("user is already a member of this household")
        }
        if (repo.findPendingInvitation(householdId, invitedUserId) != null) {
            throw DomainException("user already has a pending invitation to this household")
        }

        val invitation = Invitation(
            id = UUID.randomUUID(),
            householdId = householdId,
            invitedBy = invitedBy,
            invitedUserId = invitedUserId,
            status = InvitationStatus.PENDING,
            createdAt = clock.instant(),
        )
        repo.saveInvitation(invitation)
        return invitation
    }

    suspend fun acceptInvitation(invitationId: UUID, userId: UUID) {
        transactions.inTransaction {
            val invitation = requireInvitationFor(invitationId, userId)
            if (invitation.status != InvitationStatus.PENDING) throw DomainException("invitation is no longer valid")

            repo.saveMember(HouseholdMember(invitation.householdId, userId, HouseholdRole.MEMBER, clock.instant()))
            repo.saveInvitation(invitation.copy(status = InvitationStatus.ACCEPTED))
        }
    }

    suspend fun declineInvitation(invitationId: UUID, userId: UUID) {
        val invitation = requireInvitationFor(invitationId, userId)
        repo.saveInvitation(invitation.copy(status = InvitationStatus.DECLINED))
    }

    suspend fun listPendingInvitations(userId: UUID): List<Invitation> =
        repo.listPendingInvitations(userId)

    private suspend fun requireInvitationFor(invitationId: UUID, userId: UUID): Invitation {
        val invitation = repo.findInvitation(invitationId) ?: throw DomainException("invitation not found")
        if (invitation.invitedUserId != userId) {
            throw DomainException("forbidden: this invitation was not issued for your account")
        }
        return invitation
    }
}
