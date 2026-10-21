package com.luisteixeira.familyplanner.wiring

import com.luisteixeira.familyplanner.auth.AuthModule
import com.luisteixeira.familyplanner.auth.AuthService
import com.luisteixeira.familyplanner.calendar.CalendarModule
import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.core.module.PLATFORM_MODULE
import com.luisteixeira.familyplanner.core.module.templateRoot
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.household.HouseholdModule
import com.luisteixeira.familyplanner.household.HouseholdService
import com.luisteixeira.familyplanner.shopping.ShoppingModule
import com.luisteixeira.familyplanner.shopping.ShoppingService

data class AppContext(
    val templates: TemplateRegistry,
    val auth: AuthService,
    val household: HouseholdService,
    val shopping: ShoppingService,
    val calendar: CalendarService,
)

private val assetModules = listOf(
    PLATFORM_MODULE,
    AuthModule.NAME,
    HouseholdModule.NAME,
    ShoppingModule.NAME,
    CalendarModule.NAME,
)

fun createAppContext(storage: Storage): AppContext {
    val auth = AuthService(storage.auth)
    val household = HouseholdService(storage.household, auth, storage.transactions)
    val shopping = ShoppingService(storage.shopping)
    val calendar = CalendarService(storage.calendar)
    val templates = TemplateRegistry(assetModules.map(::templateRoot))

    return AppContext(
        templates,
        auth,
        household,
        shopping,
        calendar
    )

}
