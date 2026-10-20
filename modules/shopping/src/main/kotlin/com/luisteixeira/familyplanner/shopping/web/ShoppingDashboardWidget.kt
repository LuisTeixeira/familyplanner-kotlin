package com.luisteixeira.familyplanner.shopping.web

import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.household.DashboardWidget
import com.luisteixeira.familyplanner.shopping.ShoppingService
import java.util.UUID

class ShoppingDashboardWidget(
    private val service: ShoppingService,
    private val templates: TemplateRegistry,
) : DashboardWidget {

    override val order = 10

    override suspend fun render(householdId: UUID): String =
        templates.render(
            "shopping_app_card.html",
            mapOf("householdId" to householdId, "summary" to service.householdSummary(householdId)),
        )
}
