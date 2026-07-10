@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.shoppilist.shared.data.repository

import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

interface ListMemberRepository {
    fun getMembersForList(listId: String): Flow<List<ListMemberEntity>>
    suspend fun getMember(listId: String, userId: String): ListMemberEntity?
    suspend fun addMember(listId: String, userId: String, role: ListRole): Result<Unit>
    suspend fun removeMember(listId: String, userId: String): Result<Unit>
}

class RoomListMemberRepository(
    private val listMemberDao: ListMemberDao,
    private val shoppingItemDao: ShoppingItemDao
) : ListMemberRepository {

    override fun getMembersForList(listId: String): Flow<List<ListMemberEntity>> =
        listMemberDao.getMembersForList(listId)

    override suspend fun getMember(listId: String, userId: String): ListMemberEntity? =
        listMemberDao.getMember(listId, userId)

    override suspend fun addMember(listId: String, userId: String, role: ListRole): Result<Unit> {
        return try {
            listMemberDao.upsert(
                ListMemberEntity(
                    memberId = Uuid.random().toString(),
                    listId = listId,
                    userId = userId,
                    role = role
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeMember(listId: String, userId: String): Result<Unit> {
        return try {
            // Items this member claimed revert to unassigned when they leave the list (§2.11).
            shoppingItemDao.unassignAllForUserInList(listId, userId)
            listMemberDao.remove(listId, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

interface PresenceRepository {
    fun getForList(listId: String): Flow<List<PresenceEntity>>
    suspend fun markActive(listId: String, userId: String): Result<Unit>
}

class RoomPresenceRepository(private val presenceDao: PresenceDao) : PresenceRepository {
    override fun getForList(listId: String): Flow<List<PresenceEntity>> = presenceDao.getForList(listId)

    override suspend fun markActive(listId: String, userId: String): Result<Unit> {
        return try {
            presenceDao.upsert(PresenceEntity(listId, userId, currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

interface InvitationRepository {
    fun getInvitesForList(listId: String): Flow<List<InvitationEntity>>
    /** Pending invites addressed to this contact (recipient's email or phone) — Home banner. */
    fun getPendingForContact(contact: String): Flow<List<InvitationEntity>>
    suspend fun createInvite(
        listId: String,
        inviterUserId: String,
        inviteeContact: String,
        channel: String,
        role: ListRole
    ): Result<InvitationEntity>
    suspend fun findByToken(token: String): InvitationEntity?
    suspend fun accept(token: String, userId: String): Result<Unit>
}

class RoomInvitationRepository(
    private val invitationDao: InvitationDao,
    private val listMemberRepository: ListMemberRepository
) : InvitationRepository {

    override fun getInvitesForList(listId: String): Flow<List<InvitationEntity>> =
        invitationDao.getInvitesForList(listId)

    override fun getPendingForContact(contact: String): Flow<List<InvitationEntity>> =
        invitationDao.getPendingForContact(contact)

    override suspend fun createInvite(
        listId: String,
        inviterUserId: String,
        inviteeContact: String,
        channel: String,
        role: ListRole
    ): Result<InvitationEntity> {
        return try {
            val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
            val expires = currentTimeMillis() + sevenDaysMillis
            val invite = InvitationEntity(
                inviteId = Uuid.random().toString(),
                listId = listId,
                inviterUserId = inviterUserId,
                inviteeContact = inviteeContact,
                channel = channel,
                role = role,
                expiresAt = expires
            )
            invitationDao.upsert(invite)
            // Real SMS/email delivery isn't wired up (no backend in this project); this mirrors
            // the sample-quality SyncManager approach elsewhere in the codebase.
            Result.success(invite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByToken(token: String): InvitationEntity? = invitationDao.findByToken(token)

    override suspend fun accept(token: String, userId: String): Result<Unit> {
        return try {
            val invite = invitationDao.findByToken(token) ?: return Result.failure(IllegalArgumentException("Invite not found"))
            if (invite.status != "PENDING") return Result.failure(IllegalStateException("This invite is no longer active"))
            val expiresAt = invite.expiresAt
            if (expiresAt != null && expiresAt < currentTimeMillis()) {
                invitationDao.updateStatus(invite.inviteId, "EXPIRED")
                return Result.failure(IllegalStateException("This invite has expired"))
            }
            val listId = invite.listId ?: return Result.failure(IllegalStateException("Invite has no list"))
            listMemberRepository.addMember(listId, userId, invite.role)
            invitationDao.updateStatus(invite.inviteId, "ACCEPTED")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
