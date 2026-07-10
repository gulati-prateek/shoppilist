package com.shoppilist.backend

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.shoppilist.shared.backend.AdminBackend
import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.backend.CustomItemReport
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.backend.RemoteCatalogItem
import com.shoppilist.shared.backend.RemoteProfile
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.session.StoredLocation
import kotlinx.coroutines.tasks.await

/**
 * The one real (Android) implementation of the cloud interfaces — see the data-model doc on
 * [com.shoppilist.shared.backend.CatalogBackend]. Firestore's built-in offline persistence
 * backs the fire-and-forget writes: they queue locally and sync when a connection returns.
 */
class FirestoreBackend : CatalogBackend, ProfileBackend, AdminBackend {

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

    private fun slug(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "item" }
}
