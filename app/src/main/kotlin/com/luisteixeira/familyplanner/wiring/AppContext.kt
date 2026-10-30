package com.luisteixeira.familyplanner.wiring

import com.luisteixeira.familyplanner.auth.AuthModule
import com.luisteixeira.familyplanner.auth.session.SessionService
import com.luisteixeira.familyplanner.auth.users.UserService
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
    val users: UserService,
    val sessions: SessionService,
    val household: HouseholdService,
    val shopping: ShoppingService,
    val calendar: CalendarService,
    val secureCookies: Boolean,
)

private val assetModules = listOf(
    PLATFORM_MODULE,
    AuthModule.NAME,
    HouseholdModule.NAME,
    ShoppingModule.NAME,
    CalendarModule.NAME,
)

fun createAppContext(storage: Storage, secureCookies: Boolean): AppContext {
    val users = UserService(storage.users)
    val sessions = SessionService(storage.sessions)
    val household = HouseholdService(storage.household, users, storage.transactions)
    val shopping = ShoppingService(storage.shopping)
    val calendar = CalendarService(storage.calendar)
    val templates = TemplateRegistry(assetModules.map(::templateRoot))

    return AppContext(templates, users, sessions, household, shopping, calendar, secureCookies)
}
