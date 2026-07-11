@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.sync

import com.shoppilist.shared.backend.CollaborationBackend
import com.shoppilist.shared.backend.RemoteInvite
import com.shoppilist.shared.backend.RemoteList
import com.shoppilist.shared.backend.RemoteListItem
import com.shoppilist.shared.backend.RemoteMember
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.local.InvitationEntity
import com.shoppilist.shared.data.local.ListMemberDao
import com.shoppilist.shared.data.local.ListMemberEntity
import com.shoppilist.shared.data.local.ListRole
import com.shoppilist.shared.data.local.ShoppingItemDao
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.ShoppingListDao
import com.shoppilist.shared.data.local.ShoppingListEntity
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * Phase 4: keeps shared lists in sync between the local Room database and Firestore. Room is the
 * offline source of truth; this mirrors remote shared lists DOWN into Room (pull) and provides
 * explicit push helpers the ViewModels call on mutations (push). Pushes are explicit — never driven
 * by observing Room — so mirroring can't loop back into a push.
 *
 * A list only lives in Firestore once it's been shared (an invite was sent); local-only lists are
 * never touched by the pull because they don't appear in [CollaborationBackend.observeMyListIds].
 */
class CollaborationSyncManager(
    private val backend: CollaborationBackend,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val listDao: ShoppingListDao,
    private val itemDao: ShoppingItemDao,
    private val memberDao: ListMemberDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false
    // One mirror Job per shared list, so its snapshot listeners can be torn down when the list is
    // no longer shared with this user. Only ever touched from the single observeMyListIds collector.
    private val mirrorJobs = mutableMapOf<String, Job>()

    /** Begins mirroring the signed-in user's shared lists from Firestore into Room. Idempotent. */
    fun start() {
        if (started || !backend.isAvailable) return
        val uid = sessionManager.currentUserId.value ?: return
        started = true
        scope.launch {
            backend.observeMyListIds(uid).collect { listIds ->
                val current = listIds.toSet()
                // Start mirroring newly-shared lists.
                current.forEach { id -> if (id !in mirrorJobs) mirrorJobs[id] = mirror(id) }
                // Tear down listeners for lists no longer shared with this user (un-share / removed).
                (mirrorJobs.keys - current).forEach { id -> mirrorJobs.remove(id)?.cancel() }
            }
        }
    }

    /** Launches the three per-list snapshot collectors as children of one Job, so cancelling the
     *  returned Job stops all of them. */
    private fun mirror(listId: String): Job = scope.launch {
        launch { backend.observeList(listId).collect { rl -> rl?.let { upsertList(it) } } }
        launch { backend.observeItems(listId).collect { remoteItems -> mirrorItems(listId, remoteItems) } }
        launch { backend.observeMembers(listId).collect { members -> mirrorMembers(listId, members) } }
    }

    private suspend fun upsertList(rl: RemoteList) {
        val existing = listDao.getListOnce(rl.id)
        listDao.upsert(
            (existing ?: ShoppingListEntity(
                listId = rl.id, name = rl.name, description = rl.description,
                ownerId = rl.ownerId, householdId = null
            )).copy(name = rl.name, description = rl.description, colorHex = rl.colorHex, ownerId = rl.ownerId)
        )
    }

    private suspend fun mirrorItems(listId: String, remote: List<RemoteListItem>) {
        val remoteIds = remote.map { it.id }.toSet()
        remote.forEach { r ->
            itemDao.upsert(
                ShoppingItemEntity(
                    itemId = r.id, listId = listId, name = r.name, quantity = r.quantity, unit = r.unit,
                    categoryId = r.categoryId, checked = r.checked, checkedBy = r.checkedBy, checkedAt = r.checkedAt,
                    addedBy = r.addedBy, notes = r.notes
                )
            )
        }
        // Reconcile deletions made on another device.
        itemDao.getItemsForListOnce(listId).forEach { local ->
            if (local.itemId !in remoteIds) itemDao.deleteItem(local.itemId)
        }
    }

    private suspend fun mirrorMembers(listId: String, members: List<RemoteMember>) {
        members.forEach { m ->
            if (memberDao.getMember(listId, m.userId) == null) {
                memberDao.upsert(
                    ListMemberEntity(
                        memberId = Uuid.random().toString(), listId = listId, userId = m.userId,
                        role = runCatching { ListRole.valueOf(m.role) }.getOrDefault(ListRole.EDITOR)
                    )
                )
            }
            // Make the member's name resolvable locally without clobbering an existing rich profile.
            if (!m.name.isNullOrBlank() && userDao.getUserOnce(m.userId) == null) {
                userDao.upsert(
                    UserEntity(
                        userId = m.userId, fullName = m.name, phone = null, email = null, country = null,
                        state = null, city = null, pincode = null, profileImageUrl = null
                    )
                )
            }
        }
    }

    // ---- Push (called explicitly from ViewModels on mutations) ----

    /** Pushes the whole list + its current items + members to Firestore and creates the invite doc
     *  (keyed by recipient contact so their device finds it). */
    fun shareAndInvite(invite: InvitationEntity) {
        if (!backend.isAvailable) return
        val listId = invite.listId ?: return
        scope.launch {
            val list = listDao.getListOnce(listId) ?: return@launch
            pushListSnapshot(list)
            // Mark the list as shared LOCALLY right away by inserting the owner's own member row.
            // Without this, isShared() stays false until the owner-member push round-trips back
            // through observeMembers, so items added while the invite is still pending would not be
            // pushed individually. mirrorMembers() guards on getMember(), so this won't duplicate.
            if (memberDao.getMember(listId, list.ownerId) == null) {
                memberDao.upsert(
                    ListMemberEntity(
                        memberId = Uuid.random().toString(), listId = listId,
                        userId = list.ownerId, role = ListRole.OWNER
                    )
                )
            }
            val inviterName = userDao.getUserOnce(invite.inviterUserId)?.fullName
            backend.createInvite(
                RemoteInvite(
                    id = invite.inviteId, listId = listId, listName = list.name,
                    inviterUserId = invite.inviterUserId, inviterName = inviterName,
                    inviteeContact = invite.inviteeContact, channel = invite.channel,
                    role = invite.role.name, status = invite.status,
                    createdAt = invite.createdAt, expiresAt = invite.expiresAt
                )
            )
        }
    }

    private suspend fun pushListSnapshot(list: ShoppingListEntity) {
        val ownerName = userDao.getUserOnce(list.ownerId)?.fullName
        backend.pushList(list.toRemote())
        backend.pushMember(list.listId, RemoteMember(list.ownerId, ListRole.OWNER.name, ownerName, currentTimeMillis()))
        itemDao.getItemsForListOnce(list.listId).forEach { backend.pushItem(list.listId, it.toRemote()) }
    }

    /** Pushes a single item change (add/edit/check) for a shared list. */
    fun pushItem(listId: String, item: ShoppingItemEntity) {
        if (backend.isAvailable) scope.launch { if (isShared(listId)) backend.pushItem(listId, item.toRemote()) }
    }

    fun deleteItem(listId: String, itemId: String) {
        if (backend.isAvailable) scope.launch { if (isShared(listId)) backend.deleteItem(listId, itemId) }
    }

    fun pushListMeta(list: ShoppingListEntity) {
        if (backend.isAvailable) scope.launch { if (isShared(list.listId)) backend.pushList(list.toRemote()) }
    }

    /** Accepts a Firestore invite (adds the user as a member remotely); the pull then brings the
     *  list into Room. */
    suspend fun acceptInvite(invite: RemoteInvite): Result<Unit> {
        val uid = sessionManager.requireUserId()
        val name = userDao.getUserOnce(uid)?.fullName
        return backend.acceptInvite(invite, uid, name)
    }

    fun observePendingInvites(contacts: List<String>) = backend.observePendingInvites(contacts)

    /** A list is "shared" once it has any member row (share pushes the owner as a member). */
    private suspend fun isShared(listId: String): Boolean =
        memberDao.getMembersForList(listId).first().isNotEmpty()

    private fun ShoppingListEntity.toRemote() = RemoteList(
        id = listId, name = name, description = description, colorHex = colorHex,
        ownerId = ownerId, createdAt = createdAt, updatedAt = currentTimeMillis()
    )

    private fun ShoppingItemEntity.toRemote() = RemoteListItem(
        id = itemId, name = name, quantity = quantity, unit = unit, categoryId = categoryId,
        checked = checked, checkedBy = checkedBy, checkedAt = checkedAt, addedBy = addedBy,
        notes = notes, updatedAt = currentTimeMillis()
    )
}
