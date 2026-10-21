package com.luisteixeira.familyplanner.calendar.web

import com.luisteixeira.familyplanner.calendar.CalendarService
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.household.DashboardWidget
import java.util.UUID

class CalendarDashboardWidget(
    private val service: CalendarService,
    private val templates: TemplateRegistry,
) : DashboardWidget {

    override val order = 20

    override suspend fun render(householdId: UUID): String =
        templates.render(
            "calendar_app_card.html",
            mapOf("householdId" to householdId, "summary" to service.summary(householdId),)
        )
}