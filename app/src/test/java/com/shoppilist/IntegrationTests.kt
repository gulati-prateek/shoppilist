package com.shoppilist

import com.shoppilist.shared.affiliate.AffiliateUrlBuilder
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for key end-to-end flows
 */
class IntegrationFlowsTest {

    /**
     * Test affiliate URL building for multiple platforms
     */
    @Test
    fun testAffiliateLinkGeneration() {
        val itemName = "milk"

        val amazonUrl = AffiliateUrlBuilder.amazonSearchUrl(itemName, "affiliate_tag")
        assertNotNull(amazonUrl)
        assertTrue(amazonUrl.contains("milk"))
        assertTrue(amazonUrl.contains("tag=affiliate_tag"))

        val flipkartUrl = AffiliateUrlBuilder.flipkartSearchUrl(itemName, "token")
        assertNotNull(flipkartUrl)
        assertTrue(flipkartUrl.contains("milk"))

        val bigbasketUrl = AffiliateUrlBuilder.bigbasketSearchUrl(itemName)
        assertNotNull(bigbasketUrl)
        assertTrue(bigbasketUrl.contains("milk"))
    }
}

/**
 * Test scenario for buy online flow: item -> affiliate platform selection -> URL open
 */
class BuyOnlineFlowTest {
    @Test
    fun testBuyOnlineUURLs() {
        val platforms = mapOf(
            "amazon" to AffiliateUrlBuilder.amazonSearchUrl("bread", "tag"),
            "flipkart" to AffiliateUrlBuilder.flipkartSearchUrl("bread", "token"),
            "bigbasket" to AffiliateUrlBuilder.bigbasketSearchUrl("bread")
        )

        platforms.forEach { (platform, url) ->
            assertTrue(url.isNotEmpty())
            assertTrue(url.startsWith("https://"))
            assertTrue(url.contains("bread"))
        }
    }
}
