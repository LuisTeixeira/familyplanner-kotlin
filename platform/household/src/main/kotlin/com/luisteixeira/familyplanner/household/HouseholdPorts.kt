package com.luisteixeira.familyplanner.household

import java.util.UUID

interface HouseholdAccess {
    suspend fun isMember(householdId: UUID, userId: UUID): Boolean
}

interface DashboardWidget {
    val order: Int
    suspend fun render(householdId: UUID): String
}
