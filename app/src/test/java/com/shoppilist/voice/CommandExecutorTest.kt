package com.shoppilist.voice

import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.domain.*
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
    fun `delete item intent calls deleteItemUseCase`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(deleteItemUseCase.invoke("bread")).thenReturn(Result.success(Unit))

        val intent = VoiceIntent.DeleteItem(listName = "Groceries", itemName = "bread")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
        verify(deleteItemUseCase).invoke("bread")
        Unit
    }

    @Test
    fun `mark purchased intent calls markItemCheckedUseCase`() = runBlocking {
        val list = ShoppingListEntity(listId = "list123", name = "Groceries", description = null, ownerId = "test_user", householdId = null)
        whenever(getAllListsUseCase()).thenReturn(flowOf(listOf(list)))
        whenever(markItemCheckedUseCase.invoke("rice", true)).thenReturn(Result.success(Unit))

        val intent = VoiceIntent.MarkPurchased(listName = "Groceries", itemName = "rice")
        val res = executor.execute(intent)

        assertTrue(res is ExecutionResult.Success)
        verify(markItemCheckedUseCase).invoke("rice", true)
        Unit
    }
}

