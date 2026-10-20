package com.luisteixeira.familyplanner.shopping.model

import java.time.Instant
import java.util.UUID

data class ShoppingList(
    val id: UUID,
    val householdId: UUID,
    val name: String,
    val createdAt: Instant,
)

data class ShoppingItem(
    val id: UUID,
    val listId: UUID,
    val name: String,
    val isCompleted: Boolean,
    val createdAt: Instant,
)

data class ListDetail(
    val list: ShoppingList,
    val items: List<ShoppingItem>,
)

data class ItemCounts(
    val total: Int,
    val pending: Int,
)

data class ListSummary(
    val id: UUID,
    val householdId: UUID,
    val name: String,
    val pendingItems: Int,
    val totalItems: Int,
    val createdAt: Instant,
)

data class HouseholdSummary(
    val totalLists: Int,
    val pendingItems: Int,
)
