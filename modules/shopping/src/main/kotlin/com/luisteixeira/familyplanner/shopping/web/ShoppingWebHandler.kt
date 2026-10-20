package com.luisteixeira.familyplanner.shopping.web

import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.TemplateRegistry
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.isHtmx
import com.luisteixeira.familyplanner.core.web.redirectSeeOther
import com.luisteixeira.familyplanner.core.web.respondHtml
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import com.luisteixeira.familyplanner.household.householdId
import com.luisteixeira.familyplanner.shopping.ShoppingService
import com.luisteixeira.familyplanner.shopping.model.ListSummary
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters

class ShoppingWebHandler(
    private val service: ShoppingService,
    private val templates: TemplateRegistry,
) {

    suspend fun createList(call: ApplicationCall) {
        val householdId = call.householdId()
        val name = call.receiveParameters()["name"].orEmpty()

        if (name.isEmpty()) throw HttpError(HttpStatusCode.BadRequest, "List name is required")

        val list = httpCatch(HttpStatusCode.BadRequest) { service.createList(householdId, name) }

        if (call.isHtmx()) {
            val summary = ListSummary(list.id, list.householdId, list.name, 0, 0, list.createdAt)
            call.respondHtml(templates.render("shopping_list_card.html", mapOf("list" to summary)))
            return
        }
        call.redirectSeeOther("/app/households/$householdId/shopping/lists")
    }

    suspend fun getList(call: ApplicationCall) {
        val householdId = call.householdId()
        val listId = call.parameters["listID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid List ID")

        val detail = httpCatch(HttpStatusCode.NotFound) { service.getList(householdId, listId) }

        call.respondHtml(
            templates.render(
                "shopping_list_detail.html",
                mapOf("householdId" to householdId, "list" to detail.list, "items" to detail.items),
            ),
        )
    }

    suspend fun addItem(call: ApplicationCall) {
        val householdId = call.householdId()
        val listId = call.parameters["listID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid List ID")
        val name = call.receiveParameters()["name"].orEmpty()

        if (name.isEmpty()) throw HttpError(HttpStatusCode.BadRequest, "Item name is required")

        val item = httpCatch(HttpStatusCode.BadRequest) { service.addItem(householdId, listId, name) }

        if (call.isHtmx()) {
            call.respondHtml(
                templates.render("shopping_item_row.html", mapOf("householdId" to householdId, "item" to item)),
            )
            return
        }
        call.redirectSeeOther("/app/households/$householdId/shopping/lists/$listId")
    }

    suspend fun toggleItem(call: ApplicationCall) {
        val householdId = call.householdId()
        val itemId = call.parameters["itemID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid Item ID")

        val item = httpCatch(HttpStatusCode.NotFound) { service.toggleItem(householdId, itemId) }

        if (call.isHtmx()) {
            call.respondHtml(
                templates.render("shopping_item_row.html", mapOf("householdId" to householdId, "item" to item)),
            )
            return
        }
        call.respondHtml("", HttpStatusCode.OK)
    }

    suspend fun listDashboard(call: ApplicationCall) {
        val householdId = call.householdId()

        val summaries = service.listSummaries(householdId)

        call.respondHtml(
            templates.render("shopping_dashboard.html", mapOf("householdId" to householdId, "lists" to summaries)),
        )
    }
}
