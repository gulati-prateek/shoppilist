package com.shoppilist.voice

import com.shoppilist.shared.voice.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RuleBasedProcessorTest {
    private val processor = RuleBasedProcessor()

    @Test
    fun testCreateListIntent() = runBlocking {
        val input = "Create shopping list called Monthly Grocery"
        val res = processor.process(input)
        assertTrue(res is VoiceIntentResult.Success)
        val intent = (res as VoiceIntentResult.Success).intent
        assertTrue(intent is VoiceIntent.CreateList)
        val name = (intent as VoiceIntent.CreateList).name
        assertTrue(name.contains("monthly", ignoreCase = true))
    }

    @Test
    fun testAddItemIntent() = runBlocking {
        val input = "Add milk to Monthly Grocery"
        val res = processor.process(input)
        assertTrue(res is VoiceIntentResult.Success)
        val intent = (res as VoiceIntentResult.Success).intent
        assertTrue(intent is VoiceIntent.AddItem)
        val add = intent as VoiceIntent.AddItem
        assertEquals("milk", add.itemName)
        assertEquals("monthly grocery", add.listName?.lowercase())
    }

    @Test
    fun testRemoveItemIntent() = runBlocking {
        val input = "Remove bread from Monthly Grocery"
        val res = processor.process(input)
        assertTrue(res is VoiceIntentResult.Success)
        val intent = (res as VoiceIntentResult.Success).intent
        assertTrue(intent is VoiceIntent.DeleteItem)
        val del = intent as VoiceIntent.DeleteItem
        assertEquals("bread", del.itemName)
    }

    @Test
    fun testMarkPurchasedIntent() = runBlocking {
        val input = "Mark rice as purchased"
        val res = processor.process(input)
        assertTrue(res is VoiceIntentResult.Success)
        val intent = (res as VoiceIntentResult.Success).intent
        assertTrue(intent is VoiceIntent.MarkPurchased)
        val marked = intent as VoiceIntent.MarkPurchased
        assertEquals("rice", marked.itemName)
    }

    @Test
    fun testUnknownIntent() = runBlocking {
        val input = "random nonsense text"
        val res = processor.process(input)
        assertTrue(res is VoiceIntentResult.Success)
        val intent = (res as VoiceIntentResult.Success).intent
        assertTrue(intent is VoiceIntent.Unknown)
    }

    @Test
    fun testCaseInsensitivity() = runBlocking {
        val input = "ADD MILK TO MY LIST"
        val res = processor.process(input)
        assertTrue(res is VoiceIntentResult.Success)
        val intent = (res as VoiceIntentResult.Success).intent
        assertTrue(intent is VoiceIntent.AddItem)
    }
}

