package com.shoppilist.domain

import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.repository.ShoppingItemRepository
import com.shoppilist.shared.data.repository.SuggestionRepository
import com.shoppilist.shared.domain.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Pins the item-9 hook: off-catalog adds (NoMatch) are reported for admin review; matched adds are not. */
class AddItemUseCaseReportingTest {

    @Mock private lateinit var repo: ShoppingItemRepository
    @Mock private lateinit var matcher: CategoryMatcher
    @Mock private lateinit var suggestionRepo: SuggestionRepository
    @Mock private lateinit var catalogBackend: CatalogBackend
    @Mock private lateinit var userDao: UserDao

    private lateinit var useCase: AddItemUseCase

    private val user = UserEntity(
        userId = "user1", fullName = "Prateek G", phone = null, email = null,
        country = null, state = null, city = null, pincode = null, profileImageUrl = null,
        countryCode = "IN"
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        useCase = AddItemUseCase(repo, matcher, suggestionRepo, catalogBackend, userDao)
    }

    @Test
    fun `an off-catalog item is reported with the adder's name and country`() = runBlocking {
        whenever(matcher.match("dragonfruit jam")).thenReturn(CategoryMatch.NoMatch)
        whenever(repo.addItem(any())).thenReturn(Result.success("item1"))
        whenever(userDao.getUserOnce("user1")).thenReturn(user)

        val result = useCase(listId = "list1", itemName = "dragonfruit jam", addedBy = "user1")

        assert(result.isSuccess)
        verify(catalogBackend).reportCustomItem("dragonfruit jam", "user1", "Prateek G", "IN")
    }

    @Test
    fun `a catalog-matched item is not reported`() = runBlocking {
        whenever(matcher.match("milk")).thenReturn(CategoryMatch.Confident("dairy_eggs"))
        whenever(repo.addItem(any())).thenReturn(Result.success("item2"))

        useCase(listId = "list1", itemName = "milk", addedBy = "user1")

        verify(catalogBackend, never()).reportCustomItem(any(), any(), any(), any())
    }

    @Test
    fun `anonymous adds are not reported`() = runBlocking {
        whenever(matcher.match("mystery item")).thenReturn(CategoryMatch.NoMatch)
        whenever(repo.addItem(any())).thenReturn(Result.success("item3"))

        useCase(listId = "list1", itemName = "mystery item", addedBy = null)

        verify(catalogBackend, never()).reportCustomItem(any(), any(), any(), any())
        verify(suggestionRepo, never()).recordItemAdded(any(), any(), any())
    }

    @Test
    fun `an ambiguous match still adds without reporting`() = runBlocking {
        whenever(matcher.match("milkshake mix")).thenReturn(CategoryMatch.Ambiguous("dairy_eggs"))
        whenever(repo.addItem(any())).thenReturn(Result.success("item4"))
        whenever(userDao.getUserOnce("user1")).thenReturn(user)

        val result = useCase(listId = "list1", itemName = "milkshake mix", addedBy = "user1")

        assert(result.getOrNull()?.ambiguousCategoryId == "dairy_eggs")
        verify(catalogBackend, never()).reportCustomItem(any(), any(), any(), any())
        verify(suggestionRepo).recordItemAdded(eq("user1"), eq("milkshake mix"), eq("list1"))
    }
}
