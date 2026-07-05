package com.shoppilist.domain

import com.shoppilist.shared.data.local.ItemNameCount
import com.shoppilist.shared.data.repository.SuggestionRepository
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class SuggestionEngineTest {

    @Mock
    private lateinit var repo: SuggestionRepository

    private lateinit var engine: SuggestionEngine

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        engine = SuggestionEngine(repo)
    }

    @Test
    fun `history-only suggestions rank ahead and are deduped`() = runBlocking {
        whenever(repo.getDismissalCounts("u1")).thenReturn(emptyMap())
        whenever(repo.getTopHistoryItems("u1", 16)).thenReturn(
            listOf(ItemNameCount("milk", 5), ItemNameCount("bread", 2))
        )
        whenever(repo.searchGlobalItems("", 16)).thenReturn(emptyList())
        whenever(repo.getSeasonalItems()).thenReturn(emptyList())

        val result = engine.getSuggestions("u1", countryCode = "US", queryPrefix = "", limit = 8)

        assertEquals(listOf("milk", "bread"), result)
    }

    @Test
    fun `dismissed items are demoted, not removed`() = runBlocking {
        whenever(repo.getDismissalCounts("u1")).thenReturn(mapOf("milk" to 3))
        whenever(repo.getTopHistoryItems("u1", 16)).thenReturn(
            listOf(ItemNameCount("milk", 5), ItemNameCount("bread", 2))
        )
        whenever(repo.searchGlobalItems("", 16)).thenReturn(emptyList())
        whenever(repo.getSeasonalItems()).thenReturn(emptyList())

        val result = engine.getSuggestions("u1", countryCode = "US", queryPrefix = "", limit = 8)

        assertEquals(listOf("bread", "milk"), result)
    }

    @Test
    fun `dismiss delegates to the repository`() = runBlocking {
        engine.dismiss("u1", "milk")
        org.mockito.kotlin.verify(repo).dismissSuggestion("u1", "milk")
    }
}

class FindMissingFrequentItemsUseCaseTest {

    @Mock
    private lateinit var suggestionRepo: SuggestionRepository
    @Mock
    private lateinit var itemRepo: com.shoppilist.shared.data.repository.ShoppingItemRepository
    @Mock
    private lateinit var listRepo: com.shoppilist.shared.data.repository.ShoppingListRepository

    private lateinit var useCase: FindMissingFrequentItemsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = FindMissingFrequentItemsUseCase(suggestionRepo, itemRepo, listRepo)
    }

    @Test
    fun `items already present in an active list are excluded`() = runBlocking {
        whenever(suggestionRepo.getFrequentItemNames("u1", 3)).thenReturn(listOf("milk", "eggs"))
        whenever(listRepo.getAllListsOnce()).thenReturn(
            listOf(com.shoppilist.shared.data.local.ShoppingListEntity("list1", "Weekly", null, "u1", null))
        )
        whenever(itemRepo.getItemsForListOnce("list1")).thenReturn(
            listOf(
                com.shoppilist.shared.data.local.ShoppingItemEntity(itemId = "i1", listId = "list1", name = "Milk")
            )
        )

        val missing = useCase("u1")

        assertEquals(listOf("eggs"), missing)
    }

    @Test
    fun `returns empty immediately when there's no purchase history`() = runBlocking {
        whenever(suggestionRepo.getFrequentItemNames("u1", 3)).thenReturn(emptyList())

        val missing = useCase("u1")

        assertTrue(missing.isEmpty())
        org.mockito.kotlin.verify(listRepo, org.mockito.kotlin.never()).getAllListsOnce()
        Unit
    }
}
