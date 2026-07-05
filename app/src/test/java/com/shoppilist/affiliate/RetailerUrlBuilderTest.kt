package com.shoppilist.affiliate

import com.shoppilist.shared.affiliate.RetailerUrlBuilder
import com.shoppilist.shared.data.local.SponsoredRetailerEntity
import org.junit.Assert.*
import org.junit.Test

class RetailerUrlBuilderTest {

    private fun retailer(basketApiSupported: Boolean = false) = SponsoredRetailerEntity(
        id = "sr_test",
        name = "Test Mart",
        countryCode = "US",
        logoEmoji = "🛒",
        isSponsored = true,
        searchUrlTemplate = "https://testmart.example/search?q={item}",
        basketApiSupported = basketApiSupported
    )

    @Test
    fun `search url substitutes and URL-encodes the item name`() {
        val url = RetailerUrlBuilder.searchUrl(retailer(), "whole wheat bread")
        assertEquals("https://testmart.example/search?q=whole+wheat+bread", url)
    }

    @Test
    fun `whole list plan without basket API opens first item and lists the rest manually`() {
        val plan = RetailerUrlBuilder.wholeListPlan(retailer(basketApiSupported = false), listOf("milk", "bread", "eggs"))
        assertEquals("https://testmart.example/search?q=milk", plan.urlToOpen)
        assertEquals(listOf("bread", "eggs"), plan.remainingItemsToAddManually)
    }

    @Test
    fun `whole list plan with an empty item list opens nothing`() {
        val plan = RetailerUrlBuilder.wholeListPlan(retailer(), emptyList())
        assertNull(plan.urlToOpen)
        assertTrue(plan.remainingItemsToAddManually.isEmpty())
    }

    @Test
    fun `whole list plan with basket API support leaves nothing for manual entry`() {
        val plan = RetailerUrlBuilder.wholeListPlan(retailer(basketApiSupported = true), listOf("milk", "bread"))
        assertNotNull(plan.urlToOpen)
        assertTrue(plan.remainingItemsToAddManually.isEmpty())
    }
}
