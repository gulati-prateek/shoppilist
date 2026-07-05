package com.shoppilist.data.repository

import com.shoppilist.shared.data.local.*
import com.shoppilist.shared.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Assert.*
import java.util.*

class OfflineOpManagerTest {

    @Mock
    private lateinit var mockPendingOpDao: PendingOpDao

    private lateinit var manager: RoomOfflineOpManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        manager = RoomOfflineOpManager(mockPendingOpDao)
    }

    @Test
    fun testQueueOp() = runBlocking {
        val op = PendingOpEntity(
            opId = UUID.randomUUID().toString(),
            opType = "CREATE_LIST",
            targetId = "list1",
            payload = """{"name":"test"}""",
            status = "PENDING"
        )
        manager.queueOp(op)
        verify(mockPendingOpDao).insert(op)
    }

    @Test
    fun testMarkOpSynced() = runBlocking {
        val opId = "op1"
        manager.markOpSynced(opId)
        verify(mockPendingOpDao).updateStatus(eq(opId), eq("SYNCED"), any())
    }

    @Test
    fun testMarkOpFailed() = runBlocking {
        val opId = "op2"
        manager.markOpFailed(opId)
        verify(mockPendingOpDao).updateStatus(eq(opId), eq("FAILED"), any())
    }

    @Test
    fun testClearOp() = runBlocking {
        val opId = "op3"
        manager.clearOp(opId)
        verify(mockPendingOpDao).delete(opId)
    }
}

/**
 * Tests for conflict resolution: multiple concurrent operations should be merged correctly.
 * Using a simple in-memory model to simulate merging.
 */
class ConflictResolutionTest {

    data class OperationRecord(
        val timestamp: Long,
        val clientId: String,
        val lamportClock: Int,
        val opType: String,
        val quantity: Double
    )

    /**
     * Simple last-writer-wins merge strategy for quantity.
     */
    fun mergeQuantityUpdates(ops: List<OperationRecord>): Double {
        // Sort by timestamp, then lamportClock, then clientId
        val sorted = ops.sortedWith(
            compareBy<OperationRecord> { it.timestamp }
                .thenBy { it.lamportClock }
                .thenBy { it.clientId }
        )
        val lastOp = sorted.lastOrNull() ?: return 1.0
        return if (lastOp.opType == "UPDATE_QUANTITY") lastOp.quantity else 1.0
    }

    @Test
    fun testSimpleLastWriterWins() {
        val ops = listOf(
            OperationRecord(100, "clientA", 1, "UPDATE_QUANTITY", 5.0),
            OperationRecord(200, "clientB", 1, "UPDATE_QUANTITY", 3.0)
        )
        val result = mergeQuantityUpdates(ops)
        assertEquals(3.0, result, 0.01)
    }

    @Test
    fun testLamportClockTieBreaker() {
        val ops = listOf(
            OperationRecord(100, "clientA", 1, "UPDATE_QUANTITY", 5.0),
            OperationRecord(100, "clientB", 2, "UPDATE_QUANTITY", 3.0)
        )
        val result = mergeQuantityUpdates(ops)
        assertEquals(3.0, result, 0.01)
    }

    @Test
    fun testClientIdTieBreaker() {
        val ops = listOf(
            OperationRecord(100, "clientZ", 1, "UPDATE_QUANTITY", 5.0),
            OperationRecord(100, "clientA", 1, "UPDATE_QUANTITY", 3.0)
        )
        val result = mergeQuantityUpdates(ops)
        // Ascending clientId sort, last-wins: "clientZ" sorts after "clientA", so clientZ's value wins.
        assertEquals(5.0, result, 0.01)
    }

    @Test
    fun testMultipleOperationsOrdering() {
        val ops = listOf(
            OperationRecord(100, "client1", 1, "UPDATE_QUANTITY", 2.0),
            OperationRecord(150, "client2", 1, "UPDATE_QUANTITY", 4.0),
            OperationRecord(120, "client1", 2, "UPDATE_QUANTITY", 3.0),
            OperationRecord(150, "client3", 1, "UPDATE_QUANTITY", 1.0)
        )
        val result = mergeQuantityUpdates(ops)
        // Last by timestamp is at 150, multiple clients; sort by clientId -> client3
        assertEquals(1.0, result, 0.01)
    }
}

