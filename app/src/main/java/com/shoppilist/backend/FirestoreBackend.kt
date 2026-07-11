package com.shoppilist.backend

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.backend.CollaborationBackend
import com.shoppilist.shared.backend.CustomItemReport
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.backend.RemoteActivity
import com.shoppilist.shared.backend.RemoteCatalogItem
import com.shoppilist.shared.backend.RemoteInvite
import com.shoppilist.shared.backend.RemoteList
import com.shoppilist.shared.backend.RemoteListItem
import com.shoppilist.shared.backend.RemoteMember
import com.shoppilist.shared.backend.RemoteProfile
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.session.StoredLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * The one real (Android) implementation of the cloud interfaces — see the data-model doc on
 * [com.shoppilist.shared.backend.CatalogBackend]. Firestore's built-in offline persistence
 * backs the fire-and-forget writes: they queue locally and sync when a connection returns.
 */
class FirestoreBackend : CatalogBackend, ProfileBackend, AdminBackend, CollaborationBackend {

    private val db = FirebaseFirestore.getInstance()

    override val isAvailable: Boolean = true

    // ---- CatalogBackend ----

    override suspend fun fetchCatalog(region: String): Result<List<RemoteCatalogItem>> =
        try {
            val docs = db.collection("catalog").document(region).collection("items").get().await()
            Result.success(docs.map { doc ->
                RemoteCatalogItem(
                    id = doc.id,
                    name = doc.getString("name") ?: doc.id,
                    categoryId = doc.getString("categoryId") ?: "",
                    translations = (doc.get("translations") as? Map<*, *>)
                        ?.entries?.associate { it.key.toString() to it.value.toString() } ?: emptyMap(),
                    isSeasonal = doc.getBoolean("isSeasonal") ?: false,
                    seasonMonths = (doc.get("seasonMonths") as? List<*>)?.map { it.toString() } ?: emptyList()
                )
            }.filter { it.name.isNotBlank() && it.categoryId.isNotBlank() })
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun reportCustomItem(name: String, userId: String, userName: String?, countryCode: String?) {
        val normalized = name.trim().lowercase()
        if (normalized.isBlank()) return
        val ref = db.collection("custom_items").document(slug(normalized))
        val fields = mapOf(
            "name" to normalized,
            "countryCode" to countryCode,
            "reportedBy" to userId,
            "reportedByName" to userName,
            "reportedAt" to currentTimeMillis()
        )
        // Keyed by name-slug so repeat adds update one doc instead of piling up duplicates.
        // Only a brand-new report gets status=pending — re-adds must not resurrect a report
        // an admin already approved or rejected.
        ref.get().addOnCompleteListener { task ->
            val exists = task.isSuccessful && task.result?.exists() == true
            val data = if (exists) fields else fields + ("status" to "pending")
            ref.set(data, SetOptions.merge())
        }
    }

    // ---- ProfileBackend ----

    override fun saveProfile(profile: RemoteProfile) {
        val data = mutableMapOf<String, Any?>(
            "firstName" to profile.firstName,
            "lastName" to profile.lastName,
            "email" to profile.email,
            "phone" to profile.phone,
            "address" to profile.address,
            "city" to profile.city,
            "state" to profile.state,
            "countryCode" to profile.countryCode,
            "pincode" to profile.pincode,
            "languageCode" to profile.languageCode,
            "updatedAt" to currentTimeMillis()
        )
        profile.lastLocation?.let { loc ->
            data["lastLocation"] = mapOf(
                "latitude" to loc.latitude,
                "longitude" to loc.longitude,
                "city" to loc.city,
                "state" to loc.state,
                "countryCode" to loc.countryCode,
                "addressLine" to loc.addressLine,
                "updatedAt" to (loc.updatedAt ?: currentTimeMillis())
            )
        }
        // Drop nulls: with SetOptions.merge() an explicit null VALUE still overwrites the stored
        // field, and callers routinely send partial profiles (e.g. just a location update).
        db.collection("users").document(profile.uid)
            .set(data.filterValues { it != null }, SetOptions.merge())
    }

    override suspend fun fetchProfile(uid: String): RemoteProfile? =
        try {
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) null else {
                val loc = doc.get("lastLocation") as? Map<*, *>
                RemoteProfile(
                    uid = uid,
                    firstName = doc.getString("firstName"),
                    lastName = doc.getString("lastName"),
                    email = doc.getString("email"),
                    phone = doc.getString("phone"),
                    address = doc.getString("address"),
                    city = doc.getString("city"),
                    state = doc.getString("state"),
                    countryCode = doc.getString("countryCode"),
                    pincode = doc.getString("pincode"),
                    languageCode = doc.getString("languageCode"),
                    lastLocation = loc?.let {
                        val lat = (it["latitude"] as? Number)?.toDouble()
                        val lng = (it["longitude"] as? Number)?.toDouble()
                        if (lat == null || lng == null) null else StoredLocation(
                            latitude = lat,
                            longitude = lng,
                            city = it["city"] as? String,
                            state = it["state"] as? String,
                            countryCode = it["countryCode"] as? String,
                            addressLine = it["addressLine"] as? String,
                            updatedAt = (it["updatedAt"] as? Number)?.toLong()
                        )
                    }
                )
            }
        } catch (_: Exception) {
            null
        }

    override suspend fun deleteProfile(uid: String): Result<Unit> =
        try {
            db.collection("users").document(uid).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    // ---- AdminBackend ----

    override suspend fun isAdmin(uid: String): Boolean =
        try {
            db.collection("admins").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }

    override suspend fun pendingCustomItems(): Result<List<CustomItemReport>> =
        try {
            // Sorted client-side: where(status) + orderBy(reportedAt) would demand a composite index.
            val docs = db.collection("custom_items").whereEqualTo("status", "pending").get().await()
            Result.success(docs.map { doc ->
                CustomItemReport(
                    id = doc.id,
                    name = doc.getString("name") ?: doc.id,
                    countryCode = doc.getString("countryCode"),
                    reportedByName = doc.getString("reportedByName"),
                    reportedAt = doc.getLong("reportedAt"),
                    status = doc.getString("status") ?: "pending"
                )
            }.sortedByDescending { it.reportedAt ?: 0L })
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun approveCustomItem(
        reportId: String,
        finalName: String,
        categoryId: String,
        region: String
    ): Result<Unit> =
        try {
            val name = finalName.trim().lowercase()
            require(name.isNotBlank()) { "Item name can't be empty" }
            val batch = db.batch()
            val itemRef = db.collection("catalog").document(region).collection("items").document(slug(name))
            batch.set(
                itemRef,
                mapOf(
                    "name" to name,
                    "categoryId" to categoryId,
                    "translations" to emptyMap<String, String>(),
                    "isSeasonal" to false,
                    "seasonMonths" to emptyList<String>(),
                    "addedFromReport" to reportId,
                    "addedAt" to currentTimeMillis()
                )
            )
            batch.update(
                db.collection("custom_items").document(reportId),
                mapOf("status" to "approved", "resolvedAt" to currentTimeMillis())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override suspend fun rejectCustomItem(reportId: String): Result<Unit> =
        try {
            db.collection("custom_items").document(reportId)
                .update(mapOf("status" to "rejected", "resolvedAt" to currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    // ---- CollaborationBackend (Phase 4) ----

    private fun lists() = db.collection("lists")

    override fun pushList(list: RemoteList) {
        lists().document(list.id).set(
            mapOf(
                "name" to list.name, "description" to list.description, "colorHex" to list.colorHex,
                "ownerId" to list.ownerId, "createdAt" to list.createdAt, "updatedAt" to list.updatedAt
            ).filterValues { it != null }, SetOptions.merge()
        )
    }

    override suspend fun deleteList(listId: String): Result<Unit> =
        try {
            // Deep delete: Firestore doc deletion does NOT cascade to subcollections, and leftover
            // member docs would keep the list in every member's collectionGroup membership query.
            val listRef = lists().document(listId)
            for (sub in listOf("items", "members", "activity")) {
                val docs = listRef.collection(sub).get().await().documents
                docs.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }
            listRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun pushItem(listId: String, item: RemoteListItem) {
        lists().document(listId).collection("items").document(item.id).set(
            mapOf(
                "name" to item.name, "quantity" to item.quantity, "unit" to item.unit,
                "categoryId" to item.categoryId, "checked" to item.checked, "checkedBy" to item.checkedBy,
                "checkedAt" to item.checkedAt, "addedBy" to item.addedBy, "notes" to item.notes,
                "updatedAt" to item.updatedAt
            ).filterValues { it != null }, SetOptions.merge()
        )
    }

    override fun deleteItem(listId: String, itemId: String) {
        lists().document(listId).collection("items").document(itemId).delete()
    }

    override fun pushMember(listId: String, member: RemoteMember) {
        lists().document(listId).collection("members").document(member.userId).set(
            mapOf("userId" to member.userId, "role" to member.role, "name" to member.name, "joinedAt" to member.joinedAt)
                .filterValues { it != null }, SetOptions.merge()
        )
    }

    override fun removeMember(listId: String, userId: String) {
        lists().document(listId).collection("members").document(userId).delete()
    }

    override fun pushActivity(listId: String, activity: RemoteActivity) {
        lists().document(listId).collection("activity").document(activity.id).set(
            mapOf(
                "actorUserId" to activity.actorUserId, "actorName" to activity.actorName,
                "action" to activity.action, "itemName" to activity.itemName, "detail" to activity.detail,
                "createdAt" to activity.createdAt
            ).filterValues { it != null }
        )
    }

    override fun createInvite(invite: RemoteInvite) {
        db.collection("invitations").document(invite.id).set(
            mapOf(
                "listId" to invite.listId, "listName" to invite.listName, "inviterUserId" to invite.inviterUserId,
                "inviterName" to invite.inviterName, "inviteeContact" to invite.inviteeContact,
                "channel" to invite.channel, "role" to invite.role, "status" to invite.status,
                "createdAt" to invite.createdAt, "expiresAt" to invite.expiresAt
            ).filterValues { it != null }, SetOptions.merge()
        )
    }

    override fun observeMyListIds(uid: String): Flow<List<String>> = callbackFlow {
        val reg = db.collectionGroup("members").whereEqualTo("userId", uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.documents.mapNotNull { it.reference.parent.parent?.id }.distinct())
            }
        awaitClose { reg.remove() }
    }

    override fun observeList(listId: String): Flow<RemoteList?> = callbackFlow {
        val reg = lists().document(listId).addSnapshotListener { doc, _ ->
            if (doc != null) trySend(
                if (!doc.exists()) null else RemoteList(
                    id = doc.id, name = doc.getString("name") ?: "",
                    description = doc.getString("description"), colorHex = doc.getString("colorHex"),
                    ownerId = doc.getString("ownerId") ?: "",
                    createdAt = doc.getLong("createdAt") ?: 0L, updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            )
        }
        awaitClose { reg.remove() }
    }

    override fun observeItems(listId: String): Flow<List<RemoteListItem>> = callbackFlow {
        val reg = lists().document(listId).collection("items").addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.map { d ->
                RemoteListItem(
                    id = d.id, name = d.getString("name") ?: "", quantity = d.getDouble("quantity") ?: 1.0,
                    unit = d.getString("unit"), categoryId = d.getString("categoryId"),
                    checked = d.getBoolean("checked") ?: false, checkedBy = d.getString("checkedBy"),
                    checkedAt = d.getLong("checkedAt"), addedBy = d.getString("addedBy"),
                    notes = d.getString("notes"), updatedAt = d.getLong("updatedAt") ?: 0L
                )
            })
        }
        awaitClose { reg.remove() }
    }

    override fun observeMembers(listId: String): Flow<List<RemoteMember>> = callbackFlow {
        val reg = lists().document(listId).collection("members").addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.map { d ->
                RemoteMember(
                    userId = d.getString("userId") ?: d.id, role = d.getString("role") ?: "EDITOR",
                    name = d.getString("name"), joinedAt = d.getLong("joinedAt") ?: 0L
                )
            })
        }
        awaitClose { reg.remove() }
    }

    override fun observeActivity(listId: String): Flow<List<RemoteActivity>> = callbackFlow {
        val reg = lists().document(listId).collection("activity").addSnapshotListener { snap, _ ->
            if (snap != null) trySend(snap.documents.map { d ->
                RemoteActivity(
                    id = d.id, actorUserId = d.getString("actorUserId") ?: "",
                    actorName = d.getString("actorName") ?: "Someone", action = d.getString("action") ?: "",
                    itemName = d.getString("itemName"), detail = d.getString("detail"),
                    createdAt = d.getLong("createdAt") ?: 0L
                )
            }.sortedByDescending { it.createdAt })
        }
        awaitClose { reg.remove() }
    }

    override fun observePendingInvites(contacts: List<String>): Flow<List<RemoteInvite>> = callbackFlow {
        val filtered = contacts.filter { it.isNotBlank() }.take(10)
        if (filtered.isEmpty()) { trySend(emptyList()); awaitClose { }; return@callbackFlow }
        val reg = db.collection("invitations")
            .whereIn("inviteeContact", filtered)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.documents.map { d ->
                    RemoteInvite(
                        id = d.id, listId = d.getString("listId") ?: "", listName = d.getString("listName"),
                        inviterUserId = d.getString("inviterUserId") ?: "", inviterName = d.getString("inviterName"),
                        inviteeContact = d.getString("inviteeContact") ?: "", channel = d.getString("channel"),
                        role = d.getString("role") ?: "EDITOR", status = d.getString("status") ?: "PENDING",
                        createdAt = d.getLong("createdAt") ?: 0L, expiresAt = d.getLong("expiresAt")
                    )
                })
            }
        awaitClose { reg.remove() }
    }

    override fun declineInvite(inviteId: String) {
        db.collection("invitations").document(inviteId)
            .update(mapOf("status" to "DECLINED", "declinedAt" to currentTimeMillis()))
    }

    override suspend fun acceptInvite(invite: RemoteInvite, userId: String, userName: String?): Result<Unit> =
        try {
            val batch = db.batch()
            batch.set(
                lists().document(invite.listId).collection("members").document(userId),
                mapOf("userId" to userId, "role" to invite.role, "name" to userName, "joinedAt" to currentTimeMillis())
                    .filterValues { it != null }
            )
            batch.update(db.collection("invitations").document(invite.id),
                mapOf("status" to "ACCEPTED", "acceptedAt" to currentTimeMillis()))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    private fun slug(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "item" }
}
