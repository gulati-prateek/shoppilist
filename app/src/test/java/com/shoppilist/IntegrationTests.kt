package com.shoppilist

import com.shoppilist.shared.affiliate.AffiliateUrlBuilder
import com.shoppilist.shared.voice.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Integration tests for key end-to-end flows
 */
class IntegrationFlowsTest {

    /**
     * Test the complete voice-to-database flow:
     * Voice text -> Intent parsing -> Command execution -> operation queued
     */
    @Test
    fun testVoiceFlowCreateListAndAddItem() {
        val processor = RuleBasedProcessor()
        val flow = listOf(
            "Create shopping list called Groceries",
            "Add milk to Groceries",
            "Add bread to Groceries"
        )

        val results = flow.map { text ->
            kotlinx.coroutines.runBlocking {
                processor.process(text)
            }
        }

        // Verify all processed successfully
        assertTrue(results.all { it is VoiceIntentResult.Success })

        // Verify intent types
        val createList = results[0] as VoiceIntentResult.Success
        assertTrue(createList.intent is VoiceIntent.CreateList)

        val addMilk = results[1] as VoiceIntentResult.Success
        assertTrue(addMilk.intent is VoiceIntent.AddItem)

        val addBread = results[2] as VoiceIntentResult.Success
        assertTrue(addBread.intent is VoiceIntent.AddItem)
    }

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

    /**
     * Simulate adding multiple items via voice and verify order of operations
     */
    @Test
    fun testMultipleVoiceCommandsSequence() {
        val processor = RuleBasedProcessor()
        val commands = listOf(
            "Create shopping list called Weekly Shop",
            "Add apples to Weekly Shop",
            "Add oranges to Weekly Shop",
            "Add bananas to Weekly Shop",
            "Mark apples as purchased"
        )

        kotlinx.coroutines.runBlocking {
            val results = commands.map { processor.process(it) }

            // Verify sequence
            assertEquals(1, results.filterIsInstance<VoiceIntentResult.Success>().count {
                (it.intent as? VoiceIntent.CreateList)?.name?.contains("weekly") == true
            })

            assertEquals(3, results.filterIsInstance<VoiceIntentResult.Success>().count {
                it.intent is VoiceIntent.AddItem
            })

            assertEquals(1, results.filterIsInstance<VoiceIntentResult.Success>().count {
                it.intent is VoiceIntent.MarkPurchased
            })
        }
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

