package com.shoppilist.sync

import com.shoppilist.shared.data.local.PendingOpEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Small helper to convert PendingOpEntity.payload (JSON string) into Firestore document map
 * and to determine target collection/document for the operation.
 * This is separated for unit testing without needing Android SDK or Firestore.
 */
object PendingOpProcessor {
    data class WriteTarget(val collection: String, val docId: String, val data: Map<String, Any>)

    fun toWriteTarget(op: PendingOpEntity): WriteTarget {
        // Attempt to parse payload as a JSON object
        val payloadMap: Map<String, Any> = try {
            Json.parseToJsonElement(op.payload).jsonObject.toPlainMap()
        } catch (e: Exception) {
            // fallback: store raw payload
            mapOf("rawPayload" to (op.payload ?: ""))
        }

        return when (op.opType) {
            "CREATE_LIST", "UPDATE_LIST", "DELETE_LIST", "ARCHIVE_LIST" ->
                WriteTarget("shopping_lists", op.targetId, payloadMap)
            "ADD_ITEM", "UPDATE_ITEM", "DELETE_ITEM", "MARK_ITEM_CHECKED" ->
                WriteTarget("shopping_items", op.targetId, payloadMap)
            else -> WriteTarget("operations_log", op.opId, payloadMap + mapOf("type" to op.opType))
        }
    }
}

private fun JsonObject.toPlainMap(): Map<String, Any> = mapValues { (_, v) -> v.toPlainValue() }

private fun JsonElement.toPlainValue(): Any = when (this) {
    is JsonNull -> ""
    is JsonPrimitive -> booleanOrNull ?: doubleOrNull ?: content
    is JsonArray -> map { it.toPlainValue() }
    is JsonObject -> toPlainMap()
}
