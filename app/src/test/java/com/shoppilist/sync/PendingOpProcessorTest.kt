package com.shoppilist.sync

import com.shoppilist.shared.data.local.PendingOpEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class PendingOpProcessorTest {
    @Test
    fun testCreateListMapping() {
        val payload = "{\"listId\":\"list123\",\"name\":\"Groceries\"}"
        val op = PendingOpEntity(opId = UUID.randomUUID().toString(), opType = "CREATE_LIST", targetId = "list123", payload = payload)
        val wt = PendingOpProcessor.toWriteTarget(op)
        assertEquals("shopping_lists", wt.collection)
        assertEquals("list123", wt.docId)
        assertTrue(wt.data["name"] == "Groceries")
    }

    @Test
    fun testAddItemMapping() {
        val payload = "{\"itemId\":\"item1\",\"listId\":\"list123\",\"name\":\"Milk\"}"
        val op = PendingOpEntity(opId = UUID.randomUUID().toString(), opType = "ADD_ITEM", targetId = "item1", payload = payload)
        val wt = PendingOpProcessor.toWriteTarget(op)
        assertEquals("shopping_items", wt.collection)
        assertEquals("item1", wt.docId)
        assertTrue(wt.data["name"] == "Milk")
    }

    @Test
    fun testUnknownOpMapping() {
        val payload = "raw text"
        val op = PendingOpEntity(opId = "opX", opType = "CUSTOM_OP", targetId = "tgt", payload = payload)
        val wt = PendingOpProcessor.toWriteTarget(op)
        assertEquals("operations_log", wt.collection)
        assertEquals("opX", wt.docId)
        assertTrue(wt.data.containsKey("rawPayload") || wt.data.containsKey("type"))
    }
}

