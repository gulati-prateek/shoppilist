package com.shoppilist.voice

import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.domain.*
import com.shoppilist.shared.voice.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*

class CommandExecutorTest {

    private val getAllListsUseCase: GetAllListsUseCase = mock()
    private val getListUseCase: GetListUseCase = mock()
    private val createListUseCase: CreateListUseCase = mock()
    private val addItemUseCase: AddItemUseCase = mock()
    private val deleteItemUseCase: DeleteItemUseCase = mock()
    private val markItemCheckedUseCase: MarkItemCheckedUseCase = mock()
    private val getListItemsUseCase: GetListItemsUseCase = mock()
    private val deleteListUseCase: DeleteListUseCase = mock()

    private val executor = CommandExecutor(
        getAllListsUseCase,
        getListUseCase,
        createListUseCase,
        addItemUseCase,
        deleteItemUseCase,
        markItemCheckedUseCase,
        getListItemsUseCase,
        deleteListUseCase,
        "test_user"
    )

    @Test
    fun `create list intent calls createListUseCase`() = runBlocking {
        val name = "Monthly Grocery"
        whenever(createListUseCase(name, null, "test_user")).thenReturn(Result.success("list123"))

        val intent = VoiceIntent.CreateList(name)
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
    }

    @Test
    fun `add item intent resolves list and calls addItemUseCase`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(addItemUseCase.invoke("list123", "milk", 1.0, null, null, null, "test_user"))
            .thenReturn(Result.success(AddItemResult("item1")))

        val intent = VoiceIntent.AddItem(listName = "Groceries", itemName = "milk")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
    }

    @Test
    fun `add item without list name defaults to first list`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(addItemUseCase.invoke("list123", "milk", 1.0, null, null, null, "test_user"))
            .thenReturn(Result.success(AddItemResult("item1")))

        val intent = VoiceIntent.AddItem(listName = null, itemName = "milk")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
    }

    @Test
    fun `delete item intent matches item by name and deletes by id`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        val item = ShoppingItemEntity(itemId = "item-bread", listId = "list123", name = "Bread")
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(getListItemsUseCase.invoke("list123")).thenReturn(flowOf(listOf(item)))
        whenever(deleteItemUseCase.invoke("item-bread")).thenReturn(Result.success(Unit))

        val intent = VoiceIntent.DeleteItem(listName = "Groceries", itemName = "bread")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
        verify(deleteItemUseCase).invoke("item-bread")
        Unit
    }

    @Test
    fun `mark purchased intent matches item by name across lists`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        val item = ShoppingItemEntity(itemId = "item-rice", listId = "list123", name = "Rice")
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(getListItemsUseCase.invoke("list123")).thenReturn(flowOf(listOf(item)))
        whenever(markItemCheckedUseCase.invoke("item-rice", true)).thenReturn(Result.success(Unit))

        val intent = VoiceIntent.MarkPurchased(listName = null, itemName = "rice")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
        verify(markItemCheckedUseCase).invoke("item-rice", true)
        Unit
    }

    @Test
    fun `delete item not in any list fails with helpful message`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(getListItemsUseCase.invoke("list123")).thenReturn(flowOf(emptyList()))

        val intent = VoiceIntent.DeleteItem(listName = "Groceries", itemName = "bread")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Failure)
    }
}

