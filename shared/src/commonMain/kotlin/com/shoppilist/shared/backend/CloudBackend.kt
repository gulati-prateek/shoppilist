package com.shoppilist.shared.backend

import com.shoppilist.shared.data.session.StoredLocation
import kotlinx.coroutines.flow.Flow

/**
 * Cloud (Firestore) integration, abstracted the same way as [com.shoppilist.shared.auth.AuthService]:
 * the Firebase SDK has no Kotlin/Native artifact, so the real implementation lives in `:app`
 * (`FirestoreBackend`) and iOS registers stubs that report unavailable.
 *
 * Firestore data model:
 * - `catalog/{region}/items/{itemId}` — master item catalog per region (IN/US/EU), admin-curated.
 * - `custom_items/{normalizedNameSlug}` — items users typed that matched nothing in the catalog;
 *   the admin dashboard reviews these and can promote them into `catalog/...`.
 * - `users/{uid}` — profile mirror (name/contact/address/last location), durable across devices.
 * - `admins/{uid}` — marker docs; presence of the signed-in uid grants the in-app admin UI.
 */

/** One item of the remote master catalog. */
data class RemoteCatalogItem(
    val id: String,
    val name: String,
    val categoryId: String,
    val translations: Map<String, String> = emptyMap(),
    val isSeasonal: Boolean = false,
    val seasonMonths: List<String> = emptyList()
)

/** A user-added item that wasn't in the master catalog, awaiting admin review. */
data class CustomItemReport(
    val id: String,
    val name: String,
    val countryCode: String?,
    val reportedByName: String?,
    val reportedAt: Long?,
    val status: String
)

/** Profile mirror pushed to / restored from the cloud. */
data class RemoteProfile(
    val uid: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val countryCode: String? = null,
    val pincode: String? = null,
    val languageCode: String? = null,
    val lastLocation: StoredLocation? = null
)

interface CatalogBackend {
    val isAvailable: Boolean

    /** Full item list for one region ("IN"/"US"/"EU"). */
    suspend fun fetchCatalog(region: String): Result<List<RemoteCatalogItem>>

    /** Fire-and-forget: records an off-catalog item for admin review. Implementations must not
     *  block or throw — offline writes queue locally and sync when a connection returns. */
    fun reportCustomItem(name: String, userId: String, userName: String?, countryCode: String?)
}

interface ProfileBackend {
    val isAvailable: Boolean

    /** Fire-and-forget upsert of the profile mirror (offline-queued, same as custom items). */
    fun saveProfile(profile: RemoteProfile)

    /** Restores the profile on a fresh install / new device; null when absent or offline. */
    suspend fun fetchProfile(uid: String): RemoteProfile?
}

interface AdminBackend {
    val isAvailable: Boolean

    /** Whether this uid has an `admins/{uid}` marker doc. False when offline or unavailable. */
    suspend fun isAdmin(uid: String): Boolean

    suspend fun pendingCustomItems(): Result<List<CustomItemReport>>

    /** Promotes a reviewed custom item into `catalog/{region}/items` (possibly under an
     *  admin-edited name/category) and marks the report approved. */
    suspend fun approveCustomItem(
        reportId: String,
        finalName: String,
        categoryId: String,
        region: String
    ): Result<Unit>

    suspend fun rejectCustomItem(reportId: String): Result<Unit>
}

// ---- Collaboration (Phase 4): cross-device list sharing via Firestore ----
// Firestore model: lists/{listId} + subcollections items/{itemId}, members/{uid}, activity/{id};
// top-level invitations/{inviteId} keyed so a recipient can find invites to their email/phone.

data class RemoteList(
    val id: String,
    val name: String,
    val description: String? = null,
    val colorHex: String? = null,
    val ownerId: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class RemoteListItem(
    val id: String,
    val name: String,
    val quantity: Double = 1.0,
    val unit: String? = null,
    val categoryId: String? = null,
    val checked: Boolean = false,
    val checkedBy: String? = null,
    val checkedAt: Long? = null,
    val addedBy: String? = null,
    val notes: String? = null,
    val updatedAt: Long
)

data class RemoteMember(val userId: String, val role: String, val name: String? = null, val joinedAt: Long)

data class RemoteActivity(
    val id: String,
    val actorUserId: String,
    val actorName: String,
    val action: String,
    val itemName: String? = null,
    val detail: String? = null,
    val createdAt: Long
)

data class RemoteInvite(
    val id: String,
    val listId: String,
    val listName: String? = null,
    val inviterUserId: String,
    val inviterName: String? = null,
    val inviteeContact: String,
    val channel: String? = null,
    val role: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long? = null
)

/**
 * Cross-device collaboration transport. Writes are fire-and-forget (Firestore offline-queues them);
 * observe* return real-time snapshot flows. The local Room DB stays the offline source of truth; a
 * sync layer mirrors both directions.
 */
interface CollaborationBackend {
    val isAvailable: Boolean

    // Pushes (offline-queued).
    fun pushList(list: RemoteList)
    fun deleteList(listId: String)
    fun pushItem(listId: String, item: RemoteListItem)
    fun deleteItem(listId: String, itemId: String)
    fun pushMember(listId: String, member: RemoteMember)
    fun pushActivity(listId: String, activity: RemoteActivity)
    fun createInvite(invite: RemoteInvite)

    // Real-time observation.
    /** List ids the given user is a member of. */
    fun observeMyListIds(uid: String): Flow<List<String>>
    fun observeList(listId: String): Flow<RemoteList?>
    fun observeItems(listId: String): Flow<List<RemoteListItem>>
    fun observeMembers(listId: String): Flow<List<RemoteMember>>
    fun observeActivity(listId: String): Flow<List<RemoteActivity>>
    /** Pending invites addressed to any of the signed-in user's contacts (email/phone). */
    fun observePendingInvites(contacts: List<String>): Flow<List<RemoteInvite>>

    /** Adds the user to the list's members and flips the invite to ACCEPTED (atomic). */
    suspend fun acceptInvite(invite: RemoteInvite, userId: String, userName: String?): Result<Unit>
}
