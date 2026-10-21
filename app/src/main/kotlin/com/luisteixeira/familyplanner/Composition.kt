package com.luisteixeira.familyplanner

import com.luisteixeira.familyplanner.auth.AuthModule
import com.luisteixeira.familyplanner.auth.AuthService
import com.luisteixeira.familyplanner.auth.SessionValidator
import com.luisteixeira.familyplanner.auth.api.AuthApiHandler
import com.luisteixeira.familyplanner.auth.persistence.AuthRepository
import com.luisteixeira.familyplanner.auth.persistence.InMemoryAuthRepository
import com.luisteixeira.familyplanner.auth.web.AuthWebHandler
import com.luisteixeira.familyplanner.calendar.CalendarModule
import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.calendar.persistence.CalendarRepository
import com.luisteixeira.familyplanner.calendar.persistence.InMemoryCalendarRepository
import com.luisteixeira.familyplanner.calendar.web.CalendarDashboardWidget
import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.core.module.PLATFORM_MODULE
import com.luisteixeira.familyplanner.core.module.templateRoot
import com.luisteixeira.familyplanner.core.persistence.InMemoryTransactionRunner
import com.luisteixeira.familyplanner.core.persistence.TransactionRunner
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.household.HouseholdModule
import com.luisteixeira.familyplanner.household.HouseholdService
import com.luisteixeira.familyplanner.household.api.HouseholdApiHandler
import com.luisteixeira.familyplanner.household.persistence.HouseholdRepository
import com.luisteixeira.familyplanner.household.persistence.InMemoryHouseholdRepository
import com.luisteixeira.familyplanner.household.web.HouseholdWebHandler
import com.luisteixeira.familyplanner.shopping.ShoppingModule
import com.luisteixeira.familyplanner.shopping.ShoppingService
import com.luisteixeira.familyplanner.shopping.api.ShoppingApiHandler
import com.luisteixeira.familyplanner.shopping.persistence.InMemoryShoppingRepository
import com.luisteixeira.familyplanner.shopping.persistence.ShoppingRepository
import com.luisteixeira.familyplanner.shopping.web.ShoppingDashboardWidget
import com.luisteixeira.familyplanner.shopping.web.ShoppingWebHandler

enum class Persistence { MEMORY }

data class AppConfig(val persistence: Persistence = Persistence.MEMORY) {
    companion object {
        fun fromEnvironment(): AppConfig = AppConfig(
            persistence = System.getenv("FAMILYPLANNER_PERSISTENCE")
                ?.let { Persistence.valueOf(it.uppercase()) }
                ?: Persistence.MEMORY,
        )
    }
}

class ComposedApp(
    val modules: List<AppModule>,
    val sessionValidator: SessionValidator,
)

private class Storage(
    val auth: AuthRepository,
    val household: HouseholdRepository,
    val shopping: ShoppingRepository,
    val calendar: CalendarRepository,
    val transactions: TransactionRunner,
)

private fun Persistence.storage(): Storage = when (this) {
    Persistence.MEMORY -> Storage(
        auth = InMemoryAuthRepository(),
        household = InMemoryHouseholdRepository(),
        shopping = InMemoryShoppingRepository(),
        calendar = InMemoryCalendarRepository(),
        transactions = InMemoryTransactionRunner(),
    )
}

fun composeApp(config: AppConfig = AppConfig()): ComposedApp {
    val storage = config.persistence.storage()

    val authService = AuthService(storage.auth)
    val householdService = HouseholdService(storage.household, authService, storage.transactions)
    val shoppingService = ShoppingService(storage.shopping)
    val calendarService = CalendarService(storage.calendar)

    val templates = TemplateRegistry(
        listOf(
            PLATFORM_MODULE,
            AuthModule.NAME,
            HouseholdModule.NAME,
            ShoppingModule.NAME,
            CalendarModule.NAME,
        ).map(::templateRoot),
    )

    val modules = listOf(
        AuthModule(
            AuthWebHandler(authService, templates),
            AuthApiHandler(authService),
        ),
        HouseholdModule(
            householdService,
            HouseholdWebHandler(
                householdService,
                templates,
                dashboardWidgets = listOf(
                    ShoppingDashboardWidget(shoppingService, templates),
                    CalendarDashboardWidget(calendarService, templates),
                    ),
            ),
            HouseholdApiHandler(householdService),
        ),
        ShoppingModule(
            householdService,
            ShoppingWebHandler(shoppingService, templates),
            ShoppingApiHandler(shoppingService),
        ),
        CalendarModule(
            householdService,
        )
    )

    return ComposedApp(modules, authService)
}
