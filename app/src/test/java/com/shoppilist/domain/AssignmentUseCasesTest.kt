package com.shoppilist.domain

import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.repository.NotificationRepository
import com.shoppilist.shared.data.repository.ShoppingItemRepository
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AssignItemUseCaseTest {

    @Mock private lateinit var itemRepo: ShoppingItemRepository
    @Mock private lateinit var notificationRepo: NotificationRepository

    private lateinit var useCase: AssignItemUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = AssignItemUseCase(itemRepo, notificationRepo)
    }

    @Test
    fun `assigning to someone else notifies them`() = runBlocking {
        whenever(itemRepo.assignItem("item1", "bob", "alice")).thenReturn(Result.success(Unit))

        useCase("item1", "Milk", "Weekly Groceries", "bob", "alice", "Alice")

        verify(notificationRepo).send(eq("bob"), any(), any(), anyOrNull())
        Unit
    }

    @Test
    fun `self-assigning via I'll get this does not notify`() = runBlocking {
        whenever(itemRepo.assignItem("item1", "alice", "alice")).thenReturn(Result.success(Unit))

        useCase("item1", "Milk", "Weekly Groceries", "alice", "alice", "You")

        verify(notificationRepo, never()).send(any(), any(), any(), anyOrNull())
        Unit
    }
}

class GetAssignmentSummaryUseCaseTest {

    private val useCase = GetAssignmentSummaryUseCase()

    private fun item(id: String, assignedTo: String? = null, checked: Boolean = false) = ShoppingItemEntity(
        itemId = id,
        listId = "list1",
        name = id,
        assignedTo = assignedTo,
        checked = checked
    )

    @Test
    fun `groups items by assignee and counts checked items`() {
        val items = listOf(
            item("milk", assignedTo = "bob", checked = true),
            item("bread", assignedTo = "bob", checked = false),
            item("eggs", assignedTo = "alice", checked = true),
            item("soap", assignedTo = null)
        )

        val (byAssignee, unassigned) = useCase(items)

        assertEquals(2, byAssignee.size)
        val bob = byAssignee.first { it.userId == "bob" }
        assertEquals(2, bob.items.size)
        assertEquals(1, bob.checkedCount)
        assertEquals(1, unassigned.size)
        assertEquals("soap", unassigned.first().itemId)
    }
}
