package com.shoppilist.domain

import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.data.repository.ShoppingItemRepository
import com.shoppilist.shared.data.repository.ShoppingListRepository
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DoneShoppingUseCaseTest {

    @Mock private lateinit var itemRepo: ShoppingItemRepository
    private lateinit var useCase: DoneShoppingUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = DoneShoppingUseCase(itemRepo)
    }

    @Test
    fun `returns the unchecked items for the caller to prompt with`() = runBlocking {
        val unchecked = listOf(ShoppingItemEntity(itemId = "i1", listId = "list1", name = "Milk"))
        whenever(itemRepo.getUncheckedItemsOnce("list1")).thenReturn(unchecked)

        assertEquals(unchecked, useCase("list1"))
    }
}

class CreateLeftoverListUseCaseTest {

    @Mock private lateinit var listRepo: ShoppingListRepository
    @Mock private lateinit var itemRepo: ShoppingItemRepository
    private lateinit var useCase: CreateLeftoverListUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = CreateLeftoverListUseCase(listRepo, itemRepo)
    }

    @Test
    fun `fails when there is nothing left over`() = runBlocking {
        whenever(itemRepo.getUncheckedItemsOnce("list1")).thenReturn(emptyList())

        val result = useCase("list1", "Weekly Groceries", "u1")

        assertTrue(result.isFailure)
    }

    @Test
    fun `moves unchecked items into a new leftover list with a default name`() = runBlocking {
        val unchecked = listOf(ShoppingItemEntity(itemId = "i1", listId = "list1", name = "Milk"))
        whenever(itemRepo.getUncheckedItemsOnce("list1")).thenReturn(unchecked)
        whenever(listRepo.getListOnce("list1")).thenReturn(
            ShoppingListEntity("list1", "Weekly Groceries", null, "u1", null)
        )
        whenever(listRepo.createList(any())).thenReturn(Result.success("newList"))

        val result = useCase("list1", "Weekly Groceries", "u1", leftoverName = null)

        assertTrue(result.isSuccess)
        verify(listRepo).createList(argThat { name == "Leftover from Weekly Groceries" })
        verify(itemRepo).moveItemsToList(eq(listOf("i1")), any())
        Unit
    }
}

class MergeListsUseCaseTest {

    @Mock private lateinit var listRepo: ShoppingListRepository
    @Mock private lateinit var itemRepo: ShoppingItemRepository
    private lateinit var useCase: MergeListsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = MergeListsUseCase(listRepo, itemRepo)
    }

    @Test
    fun `moves all source items into the target and deletes the source list`() = runBlocking {
        val items = listOf(
            ShoppingItemEntity(itemId = "i1", listId = "source", name = "Milk"),
            ShoppingItemEntity(itemId = "i2", listId = "source", name = "Bread")
        )
        whenever(itemRepo.getItemsForListOnce("source")).thenReturn(items)
        whenever(listRepo.deleteList("source")).thenReturn(Result.success(Unit))

        val result = useCase("source", "target")

        assertTrue(result.isSuccess)
        verify(itemRepo).moveItemsToList(eq(listOf("i1", "i2")), eq("target"))
        verify(listRepo).deleteList("source")
        Unit
    }

    @Test
    fun `an empty source list is still deleted without a pointless move call`() = runBlocking {
        whenever(itemRepo.getItemsForListOnce("source")).thenReturn(emptyList())
        whenever(listRepo.deleteList("source")).thenReturn(Result.success(Unit))

        val result = useCase("source", "target")

        assertTrue(result.isSuccess)
        verify(itemRepo, org.mockito.kotlin.never()).moveItemsToList(any(), any())
        verify(listRepo).deleteList("source")
        Unit
    }
}
