package com.luisteixeira.familyplanner.household.persistence

import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresDatabase
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresTransactionRunner
import com.luisteixeira.familyplanner.core.persistence.postgres.connectPostgres
import com.luisteixeira.familyplanner.core.persistence.postgres.migrate
import com.luisteixeira.familyplanner.household.model.Household
import com.luisteixeira.familyplanner.household.model.HouseholdMember
import com.luisteixeira.familyplanner.household.model.HouseholdRole
import com.luisteixeira.familyplanner.household.model.Invitation
import com.luisteixeira.familyplanner.household.model.InvitationStatus
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PostgresHouseholdRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "household", module = "household")
            }
        }
    }

    private val repo by lazy { PostgresHouseholdRepository(postgres.database) }
    private val transactions by lazy { PostgresTransactionRunner(postgres.database) }
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @BeforeTest
    fun cleanTables() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE household.invitations, household.household_members, household.households CASCADE")
            }
        }
    }

    private fun household() = Household(UUID.randomUUID(), "Teixeira", now)

    @Test
    fun savedHouseholdCanBeFound() = runBlocking<Unit> {
        val household = household()

        repo.saveHousehold(household)

        assertEquals(household, repo.findHousehold(household.id))
    }

    @Test
    fun memberCanBeSavedAgainWithoutDuplicating() = runBlocking<Unit> {
        val household = household()
        repo.saveHousehold(household)
        val userId = UUID.randomUUID()

        repo.saveMember(HouseholdMember(household.id, userId, HouseholdRole.OWNER, now))
        repo.saveMember(HouseholdMember(household.id, userId, HouseholdRole.MEMBER, now))

        assertTrue(repo.isMember(household.id, userId))
        assertEquals(1, repo.listHouseholdsForUser(userId).size)
    }

    @Test
    fun invitationIsPendingThenAccepted() = runBlocking<Unit> {
        val household = household()
        repo.saveHousehold(household)
        val invitee = UUID.randomUUID()
        val invitation = Invitation(
            id = UUID.randomUUID(),
            householdId = household.id,
            invitedBy = UUID.randomUUID(),
            invitedUserId = invitee,
            status = InvitationStatus.PENDING,
            createdAt = now,
        )

        repo.saveInvitation(invitation)

        assertEquals(listOf(invitation), repo.listPendingInvitations(invitee))
        assertEquals(invitation, repo.findPendingInvitation(household.id, invitee))

        repo.saveInvitation(invitation.copy(status = InvitationStatus.ACCEPTED))

        assertEquals(emptyList(), repo.listPendingInvitations(invitee))
        assertNull(repo.findPendingInvitation(household.id, invitee))
        assertEquals(InvitationStatus.ACCEPTED, repo.findInvitation(invitation.id)?.status)
    }

    @Test
    fun failedTransactionRollsBackMembership() = runBlocking<Unit> {
        val household = household()
        repo.saveHousehold(household)
        val userId = UUID.randomUUID()

        assertFailsWith<DomainException> {
            transactions.inTransaction {
                repo.saveMember(HouseholdMember(household.id, userId, HouseholdRole.MEMBER, now))
                throw DomainException("boom")
            }
        }

        assertFalse(repo.isMember(household.id, userId))
    }
}