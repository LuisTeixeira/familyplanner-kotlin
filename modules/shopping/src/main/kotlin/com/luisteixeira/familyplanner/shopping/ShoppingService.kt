package com.luisteixeira.familyplanner.shopping

import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.shopping.model.HouseholdSummary
import com.luisteixeira.familyplanner.shopping.model.ListDetail
import com.luisteixeira.familyplanner.shopping.model.ListSummary
import com.luisteixeira.familyplanner.shopping.model.ShoppingItem
import com.luisteixeira.familyplanner.shopping.model.ShoppingList
import com.luisteixeira.familyplanner.shopping.persistence.ShoppingRepository
import java.time.Clock
import java.util.UUID

class ShoppingService(
    private val repo: ShoppingRepository,
    private val clock: Clock = Clock.systemUTC(),
) {

    suspend fun createList(householdId: UUID, name: String): ShoppingList {
        if (name.isEmpty()) throw DomainException("list name is required")

        val list = ShoppingList(UUID.randomUUID(), householdId, name, clock.instant())
        repo.saveList(list)
        return list
    }

    suspend fun getList(householdId: UUID, listId: UUID): ListDetail {
        val list = requireList(householdId, listId)
        return ListDetail(list, repo.itemsByList(listId))
    }

    suspend fun listSummaries(householdId: UUID): List<ListSummary> =
        repo.listsByHousehold(householdId).map { list ->
            val counts = repo.countItems(list.id)
            ListSummary(list.id, list.householdId, list.name, counts.pending, counts.total, list.createdAt)
        }

    suspend fun householdSummary(householdId: UUID): HouseholdSummary =
        HouseholdSummary(
            totalLists = repo.countLists(householdId),
            pendingItems = repo.countPendingItems(householdId),
        )

    suspend fun addItem(householdId: UUID, listId: UUID, name: String): ShoppingItem {
        if (name.isEmpty()) throw DomainException("item name cannot be empty")
        requireList(householdId, listId)

        val item = ShoppingItem(UUID.randomUUID(), listId, name, isCompleted = false, createdAt = clock.instant())
        repo.saveItem(item)
        return item
    }

    suspend fun toggleItem(householdId: UUID, itemId: UUID): ShoppingItem {
        val item = repo.findItem(itemId) ?: throw DomainException("item not found")
        requireList(householdId, item.listId)

        val toggled = item.copy(isCompleted = !item.isCompleted)
        repo.saveItem(toggled)
        return toggled
    }

    private suspend fun requireList(householdId: UUID, listId: UUID): ShoppingList {
        val list = repo.findList(listId)
        if (list == null || list.householdId != householdId) throw DomainException("list not found")
        return list
    }
}
