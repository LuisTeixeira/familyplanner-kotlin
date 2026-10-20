package com.luisteixeira.familyplanner.shopping.api

import com.luisteixeira.familyplanner.core.web.HttpError
import com.luisteixeira.familyplanner.core.web.httpCatch
import com.luisteixeira.familyplanner.core.web.receiveJsonOr400
import com.luisteixeira.familyplanner.core.web.respondJson
import com.luisteixeira.familyplanner.core.web.toUuidOrNull
import com.luisteixeira.familyplanner.household.householdId
import com.luisteixeira.familyplanner.shopping.ShoppingService
import com.luisteixeira.familyplanner.shopping.dto.AddItemRequest
import com.luisteixeira.familyplanner.shopping.dto.CreateListRequest
import com.luisteixeira.familyplanner.shopping.dto.toDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall

class ShoppingApiHandler(private val service: ShoppingService) {

    suspend fun createList(call: ApplicationCall) {
        val householdId = call.householdId()
        val request = call.receiveJsonOr400<CreateListRequest>("Invalid payload")

        val list = httpCatch(HttpStatusCode.BadRequest) { service.createList(householdId, request.name) }

        call.respondJson(list.toDto())
    }

    suspend fun getList(call: ApplicationCall) {
        val householdId = call.householdId()
        val listId = call.parameters["listID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid List ID")

        val detail = httpCatch(HttpStatusCode.NotFound) { service.getList(householdId, listId) }

        call.respondJson(detail.toDto())
    }

    suspend fun addItem(call: ApplicationCall) {
        val householdId = call.householdId()
        val listId = call.parameters["listID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid List ID")
        val request = call.receiveJsonOr400<AddItemRequest>("Invalid payload")

        val item = httpCatch(HttpStatusCode.BadRequest) { service.addItem(householdId, listId, request.name) }

        call.respondJson(item.toDto())
    }

    suspend fun toggleItem(call: ApplicationCall) {
        val householdId = call.householdId()
        val itemId = call.parameters["itemID"].toUuidOrNull()
            ?: throw HttpError(HttpStatusCode.BadRequest, "Invalid Item ID")

        val item = httpCatch(HttpStatusCode.NotFound) { service.toggleItem(householdId, itemId) }

        call.respondJson(item.toDto())
    }
}
