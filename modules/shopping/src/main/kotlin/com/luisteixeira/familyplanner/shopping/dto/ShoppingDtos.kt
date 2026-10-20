package com.luisteixeira.familyplanner.shopping.dto

import com.luisteixeira.familyplanner.core.serialization.InstantSerializer
import com.luisteixeira.familyplanner.core.serialization.UUIDSerializer
import com.luisteixeira.familyplanner.shopping.model.ListDetail
import com.luisteixeira.familyplanner.shopping.model.ShoppingItem
import com.luisteixeira.familyplanner.shopping.model.ShoppingList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class CreateListRequest(val name: String = "")

@Serializable
data class AddItemRequest(val name: String = "")

@Serializable
data class ShoppingItemDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("list_id")
    val listId: UUID,
    val name: String,
    @SerialName("is_completed")
    val isCompleted: Boolean,
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
)

@Serializable
data class ShoppingListDto(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("household_id")
    val householdId: UUID,
    val name: String,
    @Serializable(with = InstantSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
    val items: List<ShoppingItemDto> = emptyList(),
)

fun ShoppingItem.toDto(): ShoppingItemDto = ShoppingItemDto(id, listId, name, isCompleted, createdAt)

fun ShoppingList.toDto(): ShoppingListDto = ShoppingListDto(id, householdId, name, createdAt)

fun ListDetail.toDto(): ShoppingListDto =
    ShoppingListDto(list.id, list.householdId, list.name, list.createdAt, items.map { it.toDto() })
