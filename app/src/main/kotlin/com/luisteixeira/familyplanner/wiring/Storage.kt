package com.luisteixeira.familyplanner.wiring

import com.luisteixeira.familyplanner.auth.session.InMemorySessionRepository
import com.luisteixeira.familyplanner.auth.session.PostgresSessionRepository
import com.luisteixeira.familyplanner.auth.session.SessionRepository
import com.luisteixeira.familyplanner.auth.users.InMemoryUserRepository
import com.luisteixeira.familyplanner.auth.users.PostgresUserRepository
import com.luisteixeira.familyplanner.auth.users.UserRepository
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
    val users: UserRepository,
    val sessions: SessionRepository,
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
        users = InMemoryUserRepository(),
        sessions = InMemorySessionRepository(),
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
        users = PostgresUserRepository(postgres.database),
        sessions = PostgresSessionRepository(postgres.database),
        household = PostgresHouseholdRepository(postgres.database),
        shopping = PostgresShoppingRepository(postgres.database),
        calendar = PostgresCalendarRepository(postgres.database),
        transactions = PostgresTransactionRunner(postgres.database)
    )
}