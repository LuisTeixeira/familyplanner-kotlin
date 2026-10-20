package com.luisteixeira.familyplanner.shopping.persistence

import com.luisteixeira.familyplanner.shopping.model.ItemCounts
import com.luisteixeira.familyplanner.shopping.model.ShoppingItem
import com.luisteixeira.familyplanner.shopping.model.ShoppingList
import java.util.UUID

interface ShoppingRepository {
    suspend fun saveList(list: ShoppingList)
    suspend fun findList(listId: UUID): ShoppingList?
    suspend fun listsByHousehold(householdId: UUID): List<ShoppingList>
    suspend fun countLists(householdId: UUID): Int
    suspend fun saveItem(item: ShoppingItem)
    suspend fun findItem(itemId: UUID): ShoppingItem?
    suspend fun itemsByList(listId: UUID): List<ShoppingItem>
    suspend fun countItems(listId: UUID): ItemCounts
    suspend fun countPendingItems(householdId: UUID): Int
}
