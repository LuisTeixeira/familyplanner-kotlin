package com.luisteixeira.familyplanner.shopping.persistence

import com.luisteixeira.familyplanner.core.persistence.postgres.PostgresDatabase
import com.luisteixeira.familyplanner.core.persistence.postgres.connectPostgres
import com.luisteixeira.familyplanner.core.persistence.postgres.migrate
import com.luisteixeira.familyplanner.shopping.model.ItemCounts
import com.luisteixeira.familyplanner.shopping.model.ShoppingItem
import com.luisteixeira.familyplanner.shopping.model.ShoppingList
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PostgresShoppingRepositoryTest {

    companion object {
        private val container = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine")).apply { start() }

        private val postgres: PostgresDatabase by lazy {
            connectPostgres(container.jdbcUrl, container.username, container.password).also {
                migrate(it.dataSource, schema = "shopping", module = "shopping")
            }
        }
    }

    private val repo by lazy { PostgresShoppingRepository(postgres.database) }
    private val createdAt = Instant.parse("2026-01-01T00:00:00Z")
    private val householdId = UUID.randomUUID()

    @BeforeTest
    fun cleanTables() {
        postgres.dataSource.connection.use { connection ->
            connection.createStatement().use {
                it.execute("TRUNCATE shopping.shopping_items, shopping.shopping_lists CASCADE")
            }
        }
    }

    private fun list(household: UUID = householdId, name: String = "Groceries") =
        ShoppingList(UUID.randomUUID(), household, name, createdAt)

    private fun item(listId: UUID, name: String = "Milk", completed: Boolean = false) =
        ShoppingItem(UUID.randomUUID(), listId, name, completed, createdAt)

    @Test
    fun savedListCanBeFound() = runBlocking {
        val list = list()

        repo.saveList(list)

        assertEquals(list, repo.findList(list.id))
    }

    @Test
    fun savedItemCanBeToggled() = runBlocking {
        val list = list()
        repo.saveList(list)
        val item = item(list.id)
        repo.saveItem(item)

        repo.saveItem(item.copy(isCompleted = true))

        assertEquals(true, repo.findItem(item.id)?.isCompleted)
    }

    @Test
    fun itemsByListReturnsOnlyItsOwnItems() = runBlocking {
        val groceries = list(name = "Groceries")
        val hardware = list(name = "Hardware")
        repo.saveList(groceries)
        repo.saveList(hardware)
        val milk = item(groceries.id, "Milk")
        repo.saveItem(milk)
        repo.saveItem(item(hardware.id, "Nails"))

        assertEquals(listOf(milk), repo.itemsByList(groceries.id))
    }

    @Test
    fun countItemsReportsTotalAndPending() = runBlocking {
        val list = list()
        repo.saveList(list)
        repo.saveItem(item(list.id, "Milk", completed = true))
        repo.saveItem(item(list.id, "Bread"))
        repo.saveItem(item(list.id, "Eggs"))

        assertEquals(ItemCounts(total = 3, pending = 2), repo.countItems(list.id))
    }

    @Test
    fun countListsCountsOnlyThisHousehold() = runBlocking {
        repo.saveList(list(name = "Groceries"))
        repo.saveList(list(name = "Pharmacy"))
        repo.saveList(list(household = UUID.randomUUID(), name = "Elsewhere"))

        assertEquals(2, repo.countLists(householdId))
    }

    @Test
    fun countPendingItemsCountsOnlyThisHousehold() = runBlocking {
        val mine = list()
        val other = list(household = UUID.randomUUID())
        repo.saveList(mine)
        repo.saveList(other)
        repo.saveItem(item(mine.id, "Milk"))
        repo.saveItem(item(mine.id, "Bread", completed = true))
        repo.saveItem(item(other.id, "Eggs"))

        assertEquals(1, repo.countPendingItems(householdId))
    }

    @Test
    fun missingItemIsNull() = runBlocking {
        assertNull(repo.findItem(UUID.randomUUID()))
    }
}