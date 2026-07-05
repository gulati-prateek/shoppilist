package com.shoppilist.domain

import com.shoppilist.shared.data.local.CategoryCorrectionEntity
import com.shoppilist.shared.data.local.GlobalItemEntity
import com.shoppilist.shared.data.repository.ItemCategoryRepository
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class CategoryMatcherTest {

    @Mock
    private lateinit var repo: ItemCategoryRepository

    private lateinit var matcher: CategoryMatcher

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        matcher = CategoryMatcher(repo)
    }

    @Test
    fun `a family correction wins over everything else`() = runBlocking {
        whenever(repo.getLatestCorrection("dettol")).thenReturn(
            CategoryCorrectionEntity("c1", "dettol", "personal_care", "health_pharmacy", "user1")
        )

        val result = matcher.match("Dettol")

        assertEquals(CategoryMatch.Confident("health_pharmacy"), result)
    }

    @Test
    fun `an exact global catalog match is confident`() = runBlocking {
        whenever(repo.getLatestCorrection("milk")).thenReturn(null)
        whenever(repo.findGlobalItemByName("milk")).thenReturn(
            GlobalItemEntity("gi_milk", "milk", "dairy_eggs")
        )

        val result = matcher.match("Milk")

        assertEquals(CategoryMatch.Confident("dairy_eggs"), result)
    }

    @Test
    fun `a prefix-only match is ambiguous, not silently applied`() = runBlocking {
        whenever(repo.getLatestCorrection("milkshake mix")).thenReturn(null)
        whenever(repo.findGlobalItemByName("milkshake mix")).thenReturn(null)
        whenever(repo.searchGlobalItemsByPrefix("milkshake", 1)).thenReturn(
            listOf(GlobalItemEntity("gi_milk", "milk", "dairy_eggs"))
        )

        val result = matcher.match("milkshake mix")

        assertEquals(CategoryMatch.Ambiguous("dairy_eggs"), result)
    }

    @Test
    fun `no match anywhere returns NoMatch`() = runBlocking {
        whenever(repo.getLatestCorrection("xyzzy")).thenReturn(null)
        whenever(repo.findGlobalItemByName("xyzzy")).thenReturn(null)
        whenever(repo.searchGlobalItemsByPrefix("xyzzy", 1)).thenReturn(emptyList())

        val result = matcher.match("xyzzy")

        assertEquals(CategoryMatch.NoMatch, result)
    }

    @Test
    fun `blank input is NoMatch without touching the repository`() = runBlocking {
        val result = matcher.match("   ")
        assertEquals(CategoryMatch.NoMatch, result)
    }
}
