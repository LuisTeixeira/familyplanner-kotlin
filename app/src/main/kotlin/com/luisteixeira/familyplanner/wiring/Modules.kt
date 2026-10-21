package com.luisteixeira.familyplanner.wiring

import com.luisteixeira.familyplanner.auth.AuthModule
import com.luisteixeira.familyplanner.auth.api.AuthApiHandler
import com.luisteixeira.familyplanner.auth.web.AuthWebHandler
import com.luisteixeira.familyplanner.calendar.CalendarModule
import com.luisteixeira.familyplanner.calendar.api.CalendarApiHandler
import com.luisteixeira.familyplanner.calendar.web.CalendarDashboardWidget
import com.luisteixeira.familyplanner.calendar.web.CalendarWebHandler
import com.luisteixeira.familyplanner.core.module.AppModule
import com.luisteixeira.familyplanner.household.DashboardWidget
import com.luisteixeira.familyplanner.household.HouseholdModule
import com.luisteixeira.familyplanner.household.api.HouseholdApiHandler
import com.luisteixeira.familyplanner.household.web.HouseholdWebHandler
import com.luisteixeira.familyplanner.shopping.ShoppingModule
import com.luisteixeira.familyplanner.shopping.api.ShoppingApiHandler
import com.luisteixeira.familyplanner.shopping.web.ShoppingDashboardWidget
import com.luisteixeira.familyplanner.shopping.web.ShoppingWebHandler
import java.time.ZoneId

fun AppContext.modules(): List<AppModule> = listOf(
    authModule(this),
    householdModule(this),
    shoppingModule(this),
    calendarModule(this),
)

private fun authModule(ctx: AppContext) = AuthModule(
    AuthWebHandler(ctx.auth, ctx.templates),
    AuthApiHandler(ctx.auth)
)

private fun householdModule(ctx: AppContext) = HouseholdModule (
    ctx.household,
    HouseholdWebHandler(
        ctx.household,
        ctx.templates,
        dashboardWidgets = dashboardWidgets(ctx)
    ),
    HouseholdApiHandler(ctx.household)
)

private fun shoppingModule(ctx: AppContext) = ShoppingModule(
    ctx.household,
    ShoppingWebHandler(ctx.shopping, ctx.templates),
    ShoppingApiHandler(ctx.shopping),
)

private fun calendarModule(ctx: AppContext) = CalendarModule(
    ctx.household,
    CalendarWebHandler(
        ctx.calendar,
        ctx.templates,
        ZoneId.systemDefault(),
    ),
    CalendarApiHandler(ctx.calendar, ZoneId.systemDefault())
)

private fun dashboardWidgets(ctx: AppContext): List<DashboardWidget> = listOf(
    ShoppingDashboardWidget(ctx.shopping, ctx.templates),
    CalendarDashboardWidget(ctx.calendar, ctx.templates)
)