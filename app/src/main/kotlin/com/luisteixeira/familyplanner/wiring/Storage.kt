package com.luisteixeira.familyplanner.wiring

import com.luisteixeira.familyplanner.auth.persistence.AuthRepository
import com.luisteixeira.familyplanner.auth.persistence.InMemoryAuthRepository
import com.luisteixeira.familyplanner.calendar.persistence.CalendarRepository
import com.luisteixeira.familyplanner.calendar.persistence.InMemoryCalendarRepository
import com.luisteixeira.familyplanner.config.Persistence
import com.luisteixeira.familyplanner.core.persistence.InMemoryTransactionRunner
import com.luisteixeira.familyplanner.core.persistence.TransactionRunner
import com.luisteixeira.familyplanner.household.persistence.HouseholdRepository
import com.luisteixeira.familyplanner.household.persistence.InMemoryHouseholdRepository
import com.luisteixeira.familyplanner.shopping.persistence.InMemoryShoppingRepository
import com.luisteixeira.familyplanner.shopping.persistence.ShoppingRepository

class Storage(
    val auth: AuthRepository,
    val household: HouseholdRepository,
    val shopping: ShoppingRepository,
    val calendar: CalendarRepository,
    val transactions: TransactionRunner,
)

fun Persistence.storage(): Storage = when (this) {
    Persistence.MEMORY -> Storage(
        auth = InMemoryAuthRepository(),
        household = InMemoryHouseholdRepository(),
        shopping = InMemoryShoppingRepository(),
        calendar = InMemoryCalendarRepository(),
        transactions = InMemoryTransactionRunner(),
    )

    Persistence.POSTGRES -> error("Postgres storage is not implemented yet")
}
