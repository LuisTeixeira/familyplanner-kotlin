package com.luisteixeira.familyplanner.shopping

import com.luisteixeira.familyplanner.core.domain.DomainException
import com.luisteixeira.familyplanner.shopping.persistence.InMemoryShoppingRepository
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShoppingServiceTest {

    @Test
    fun toggleFlipsCompletionAndCountsReflectIt() = runBlocking<Unit> {
        val service = ShoppingService(InMemoryShoppingRepository())
        val household = UUID.randomUUID()
        val list = service.createList(household, "Groceries")
        val item = service.addItem(household, list.id, "Milk")

        val toggled = service.toggleItem(household, item.id)

        assertEquals(true, toggled.isCompleted)
        assertEquals(0, service.householdSummary(household).pendingItems)
    }

    @Test
    fun itemsCannotBeAddedToAnotherHouseholdsList() = runBlocking<Unit> {
        val service = ShoppingService(InMemoryShoppingRepository())
        val owner = UUID.randomUUID()
        val list = service.createList(owner, "Private")

        assertFailsWith<DomainException> { service.addItem(UUID.randomUUID(), list.id, "Intruder") }
    }

    @Test
    fun itemsCannotBeToggledThroughAnotherHousehold() = runBlocking<Unit> {
        val service = ShoppingService(InMemoryShoppingRepository())
        val owner = UUID.randomUUID()
        val list = service.createList(owner, "Private")
        val item = service.addItem(owner, list.id, "Secret")

        assertFailsWith<DomainException> { service.toggleItem(UUID.randomUUID(), item.id) }
    }
}
