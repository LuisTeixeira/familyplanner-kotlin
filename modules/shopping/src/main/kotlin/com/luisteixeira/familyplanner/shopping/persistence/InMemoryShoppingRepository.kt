package com.luisteixeira.familyplanner.shopping.persistence

import com.luisteixeira.familyplanner.shopping.model.ItemCounts
import com.luisteixeira.familyplanner.shopping.model.ShoppingItem
import com.luisteixeira.familyplanner.shopping.model.ShoppingList
import java.util.UUID

class InMemoryShoppingRepository : ShoppingRepository {
    private val lock = Any()
    private val lists = HashMap<UUID, ShoppingList>()
    private val items = HashMap<UUID, ShoppingItem>()

    override suspend fun saveList(list: ShoppingList) {
        synchronized(lock) { lists[list.id] = list }
    }

    override suspend fun findList(listId: UUID): ShoppingList? = synchronized(lock) { lists[listId] }

    override suspend fun listsByHousehold(householdId: UUID): List<ShoppingList> = synchronized(lock) {
        lists.values.filter { it.householdId == householdId }
    }

    override suspend fun countLists(householdId: UUID): Int = synchronized(lock) {
        lists.values.count { it.householdId == householdId }
    }

    override suspend fun saveItem(item: ShoppingItem) {
        synchronized(lock) { items[item.id] = item }
    }

    override suspend fun findItem(itemId: UUID): ShoppingItem? = synchronized(lock) { items[itemId] }

    override suspend fun itemsByList(listId: UUID): List<ShoppingItem> = synchronized(lock) {
        items.values.filter { it.listId == listId }
    }

    override suspend fun countItems(listId: UUID): ItemCounts = synchronized(lock) {
        val listItems = items.values.filter { it.listId == listId }
        ItemCounts(total = listItems.size, pending = listItems.count { !it.isCompleted })
    }

    override suspend fun countPendingItems(householdId: UUID): Int = synchronized(lock) {
        val householdListIds = lists.values.filter { it.householdId == householdId }.map { it.id }.toSet()
        items.values.count { it.listId in householdListIds && !it.isCompleted }
    }
}
