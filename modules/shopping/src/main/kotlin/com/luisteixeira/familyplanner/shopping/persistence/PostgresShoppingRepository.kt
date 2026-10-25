package com.luisteixeira.familyplanner.shopping.persistence

import com.luisteixeira.familyplanner.shopping.model.ItemCounts
import com.luisteixeira.familyplanner.shopping.model.ShoppingItem
import com.luisteixeira.familyplanner.shopping.model.ShoppingList
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import com.luisteixeira.familyplanner.core.persistence.postgres.dbTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.ZoneOffset
import java.util.UUID

class PostgresShoppingRepository(private val database: Database) : ShoppingRepository {

    override suspend fun saveList(list: ShoppingList) {
        dbTransaction(database) {
            val updated = ShoppingListsTable.update({ ShoppingListsTable.id eq list.id }) {
                it[householdId] = list.householdId
                it[name] = list.name
            }
            if (updated == 0) {
                ShoppingListsTable.insert {
                    it[id] = list.id
                    it[householdId] = list.householdId
                    it[name] = list.name
                    it[createdAt] = list.createdAt.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findList(listId: UUID): ShoppingList? =
        dbTransaction(database) {
            ShoppingListsTable.selectAll().where { ShoppingListsTable.id eq listId }
                .singleOrNull()?.toShoppingList()
        }

    override suspend fun listsByHousehold(householdId: UUID): List<ShoppingList> =
        dbTransaction(database) {
            ShoppingListsTable.selectAll().where { ShoppingListsTable.householdId eq householdId }
                .map { it.toShoppingList() }
        }

    override suspend fun countLists(householdId: UUID): Int =
        dbTransaction(database) {
            ShoppingListsTable.selectAll().where { ShoppingListsTable.householdId eq householdId }
                .count().toInt()
        }

    override suspend fun saveItem(item: ShoppingItem) {
        dbTransaction(database) {
            val updated = ShoppingItemsTable.update({ ShoppingItemsTable.id eq item.id }) {
                it[name] = item.name
                it[isCompleted] = item.isCompleted
            }
            if (updated == 0) {
                ShoppingItemsTable.insert {
                    it[id] = item.id
                    it[listId] = item.listId
                    it[name] = item.name
                    it[isCompleted] = item.isCompleted
                    it[createdAt] = item.createdAt.atOffset(ZoneOffset.UTC)
                }
            }
        }
    }

    override suspend fun findItem(itemId: UUID): ShoppingItem? =
        dbTransaction(database) {
            ShoppingItemsTable.selectAll().where { ShoppingItemsTable.id eq itemId }
                .singleOrNull()?.toShoppingItem()
        }

    override suspend fun itemsByList(listId: UUID): List<ShoppingItem> =
        dbTransaction(database) {
            ShoppingItemsTable.selectAll().where { ShoppingItemsTable.listId eq listId }
                .map { it.toShoppingItem() }
        }

    override suspend fun countItems(listId: UUID): ItemCounts =
        dbTransaction(database) {
            val total = ShoppingItemsTable.selectAll()
                .where { ShoppingItemsTable.listId eq listId }
                .count()
            val pending = ShoppingItemsTable.selectAll()
                .where {
                    (ShoppingItemsTable.listId eq listId) and (ShoppingItemsTable.isCompleted eq false)
                }
                .count()
            ItemCounts(total = total.toInt(), pending = pending.toInt())
        }

    override suspend fun countPendingItems(householdId: UUID): Int =
        dbTransaction(database) {
            ShoppingItemsTable
                .join(ShoppingListsTable, JoinType.INNER, ShoppingItemsTable.listId, ShoppingListsTable.id)
                .selectAll()
                .where {
                    (ShoppingListsTable.householdId eq householdId) and
                            (ShoppingItemsTable.isCompleted eq false)
                }
                .count().toInt()
        }

    private fun ResultRow.toShoppingList(): ShoppingList = ShoppingList(
        id = this[ShoppingListsTable.id],
        householdId = this[ShoppingListsTable.householdId],
        name = this[ShoppingListsTable.name],
        createdAt = this[ShoppingListsTable.createdAt].toInstant(),
    )

    private fun ResultRow.toShoppingItem(): ShoppingItem = ShoppingItem(
        id = this[ShoppingItemsTable.id],
        listId = this[ShoppingItemsTable.listId],
        name = this[ShoppingItemsTable.name],
        isCompleted = this[ShoppingItemsTable.isCompleted],
        createdAt = this[ShoppingItemsTable.createdAt].toInstant(),
    )

    private object ShoppingListsTable : Table("shopping.shopping_lists") {
        val id = javaUUID("id")
        val householdId = javaUUID("household_id")
        val name = text("name")
        val createdAt = timestampWithTimeZone("created_at")

        override val primaryKey = PrimaryKey(id)
    }

    private object ShoppingItemsTable : Table("shopping.shopping_items") {
        val id = javaUUID("id")
        val listId = javaUUID("list_id")
        val name = text("name")
        val isCompleted = bool("is_completed")
        val createdAt = timestampWithTimeZone("created_at")

        override val primaryKey = PrimaryKey(id)
    }
}