package com.luisteixeira.familyplanner.household.persistence

import com.luisteixeira.familyplanner.household.model.Household
import com.luisteixeira.familyplanner.household.model.HouseholdMember
import com.luisteixeira.familyplanner.household.model.Invitation
import com.luisteixeira.familyplanner.household.model.InvitationStatus
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.ZoneOffset
import java.util.UUID

class PostgresHouseholdRepository(
    private val database: Database
) : HouseholdRepository {

    override suspend fun saveHousehold(household: Household) {
        suspendTransaction(database) {
            HouseholdsTable.insert {
                it[id] = household.id
                it[name] = household.name
                it[createdAt] = household.createdAt.atOffset(ZoneOffset.UTC)
            }
        }
    }

    override suspend fun findHousehold(householdId: UUID): Household? {
        return suspendTransaction(database) {
            HouseholdsTable.selectAll().where {
                HouseholdsTable.id eq householdId
            }.singleOrNull()?.toHousehold()
        }
    }

    override suspend fun saveMember(member: HouseholdMember) {
        suspendTransaction(database) {
            val updated = HouseholdMembersTable.update({
                (HouseholdMembersTable.householdId eq member.householdId) and
                        (HouseholdMembersTable.userId eq member.userId)
            }) {
                it[role] = member.role.name
                it[joinedAt] = member.joinedAt.atOffset(ZoneOffset.UTC)
            }
            if (updated == 0) {
                HouseholdMembersTable.insert {
                    it[householdId] = member.householdId
                    it[userId] = member.userId
                    it[role] = member.role.name
                    it[joinedAt] = member.joinedAt.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun isMember(householdId: UUID, userId: UUID): Boolean {
        return suspendTransaction(database) {
            HouseholdMembersTable.selectAll().where {
                (HouseholdMembersTable.householdId eq householdId) and
                        (HouseholdMembersTable.userId eq userId)
            }.any()
        }
    }

    override suspend fun listHouseholdsForUser(userId: UUID): List<Household> {
        return suspendTransaction(database) {
            val householdIds = HouseholdMembersTable.selectAll()
                .where { HouseholdMembersTable.userId eq userId }
                .map { it[HouseholdMembersTable.householdId] }

            if (householdIds.isEmpty()) {
                emptyList()
            } else {
                HouseholdsTable.selectAll()
                    .where { HouseholdsTable.id inList householdIds }
                    .map { it.toHousehold() }
            }
        }
    }

    override suspend fun saveInvitation(invitation: Invitation) {
        suspendTransaction(database) {
            val updated = InvitationsTable.update ({ InvitationsTable.id eq invitation.id }) {
                it[status] = invitation.status.name
            }
            if (updated == 0) {
                InvitationsTable.insert {
                    it[id] = invitation.id
                    it[householdId] = invitation.householdId
                    it[invitedBy] = invitation.invitedBy
                    it[invitedUserId] = invitation.invitedUserId
                    it[status] = invitation.status.name
                    it[createdAt] = invitation.createdAt.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findInvitation(invitationId: UUID): Invitation? {
        return suspendTransaction(database) {
            InvitationsTable.selectAll().where {
                InvitationsTable.id eq invitationId
            }.singleOrNull()?.toInvitation()
        }
    }

    override suspend fun listPendingInvitations(userId: UUID): List<Invitation> {
        return suspendTransaction(database) {
            InvitationsTable.selectAll()
                .where {
                    (InvitationsTable.invitedUserId eq userId) and
                            (InvitationsTable.status eq InvitationStatus.PENDING.name)
                }
                .map { it.toInvitation() }
        }
    }

    override suspend fun findPendingInvitation(householdId: UUID, userId: UUID): Invitation? {
        return suspendTransaction(database) {
            InvitationsTable.selectAll()
                .where {
                    (InvitationsTable.householdId eq householdId) and
                            (InvitationsTable.invitedUserId eq userId) and
                            (InvitationsTable.status eq InvitationStatus.PENDING.name)
                }
                .singleOrNull()
                ?.toInvitation()
        }
    }

    private fun ResultRow.toHousehold(): Household = Household(
        id = this[HouseholdsTable.id],
        name = this[HouseholdsTable.name],
        createdAt = this[HouseholdsTable.createdAt].toInstant(),
    )

    private fun ResultRow.toInvitation(): Invitation = Invitation(
        id = this[InvitationsTable.id],
        householdId = this[InvitationsTable.householdId],
        invitedBy = this[InvitationsTable.invitedBy],
        invitedUserId = this[InvitationsTable.invitedUserId],
        status = InvitationStatus.valueOf(this[InvitationsTable.status]),
        createdAt = this[InvitationsTable.createdAt].toInstant(),
    )

    private object HouseholdsTable : Table("household.households") {
        val id = javaUUID("id")
        val name = text("name")
        val createdAt = timestampWithTimeZone("created_at")

        override val primaryKey = PrimaryKey(id)
    }

    private object HouseholdMembersTable : Table("household.household_members") {
        val householdId = javaUUID("household_id")
        val userId = javaUUID("user_id")
        val role = text("role")
        val joinedAt = timestampWithTimeZone("joined_at")

        override val primaryKey = PrimaryKey(householdId, userId)
    }

    private object InvitationsTable : Table("household.invitations") {
        val id = javaUUID("id")
        val householdId = javaUUID("household_id")
        val invitedBy = javaUUID("invited_by")
        val invitedUserId = javaUUID("invited_user_id")
        val status = text("status")
        val createdAt = timestampWithTimeZone("created_at")

        override val primaryKey = PrimaryKey(id)
    }
}