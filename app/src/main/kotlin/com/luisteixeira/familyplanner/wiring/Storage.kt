package com.luisteixeira.familyplanner.wiring

import com.luisteixeira.familyplanner.auth.persistence.AuthRepository
import com.luisteixeira.familyplanner.auth.persistence.InMemoryAuthRepository
import com.luisteixeira.familyplanner.auth.persistence.PostgresAuthRepository
import com.luisteixeira.familyplanner.calendar.persistence.CalendarRepository
import com.luisteixeira.familyplanner.calendar.persistence.InMemoryCalendarRepository
import com.luisteixeira.familyplanner.calendar.persistence.PostgresCalendarRepository
import com.luisteixeira.familyplanner.config.DatabaseConfig
import com.luisteixeira.familyplanner.config.Persistence
import com.luisteixeira.familyplanner.core.persistence.InMemoryTransactionRunner
import com.luisteixeira.familyplanner.core.persistence.TransactionRunner
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresDatabase
import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresTransactionRunner
import com.luisteixeira.familyplanner.core.persistence.postgres.connectPostgres
import com.luisteixeira.familyplanner.core.persistence.postgres.migrate
import com.luisteixeira.familyplanner.household.persistence.HouseholdRepository
import com.luisteixeira.familyplanner.household.persistence.InMemoryHouseholdRepository
import com.luisteixeira.familyplanner.household.persistence.PostgresHouseholdRepository
import com.luisteixeira.familyplanner.shopping.persistence.InMemoryShoppingRepository
import com.luisteixeira.familyplanner.shopping.persistence.PostgresShoppingRepository
import com.luisteixeira.familyplanner.shopping.persistence.ShoppingRepository

class Storage(
    val auth: AuthRepository,
    val household: HouseholdRepository,
    val shopping: ShoppingRepository,
    val calendar: CalendarRepository,
    val transactions: TransactionRunner,
    private val pool: PostgresDatabase? = null,
) : AutoCloseable {

    override fun close() {
        pool?.close()
    }

}

private val moduleSchemas = listOf("auth", "household", "shopping", "calendar")

fun Persistence.storage(database: DatabaseConfig?): Storage = when (this) {
    Persistence.MEMORY -> Storage(
        auth = InMemoryAuthRepository(),
        household = InMemoryHouseholdRepository(),
        shopping = InMemoryShoppingRepository(),
        calendar = InMemoryCalendarRepository(),
        transactions = InMemoryTransactionRunner(),
    )

    Persistence.POSTGRES -> postgresStorage(
        requireNotNull(database) {
            "database settings are required for POSTGRES"
        },
    )
}

private fun postgresStorage(config: DatabaseConfig): Storage {
    val postgres = connectPostgres(config.url, config.user, config.password)
    moduleSchemas.forEach { migrate(postgres.dataSource, schema = it, module = it) }
    return Storage(
        auth = PostgresAuthRepository(postgres.database),
        household = PostgresHouseholdRepository(postgres.database),
        shopping = PostgresShoppingRepository(postgres.database),
        calendar = PostgresCalendarRepository(postgres.database),
        transactions = PostgresTransactionRunner(postgres.database)
    )
}