package com.shoppilist.shared.domain

import com.shoppilist.shared.data.local.InvitationEntity
import com.shoppilist.shared.data.local.ListMemberEntity
import com.shoppilist.shared.data.local.ListRole
import com.shoppilist.shared.data.local.PresenceEntity
import com.shoppilist.shared.data.repository.InvitationRepository
import com.shoppilist.shared.data.repository.ListMemberRepository
import com.shoppilist.shared.data.repository.PresenceRepository
import kotlinx.coroutines.flow.Flow

class GetListMembersUseCase(private val repo: ListMemberRepository) {
    operator fun invoke(listId: String): Flow<List<ListMemberEntity>> = repo.getMembersForList(listId)
}

class AddListMemberUseCase(private val repo: ListMemberRepository) {
    suspend operator fun invoke(listId: String, userId: String, role: ListRole): Result<Unit> =
        repo.addMember(listId, userId, role)
}

/** Removing a member also reverts any items they'd claimed back to unassigned (§2.11). */
class RemoveListMemberUseCase(private val repo: ListMemberRepository) {
    suspend operator fun invoke(listId: String, userId: String): Result<Unit> = repo.removeMember(listId, userId)
}

class CreateInviteUseCase(private val repo: InvitationRepository) {
    suspend operator fun invoke(
        listId: String,
        inviterUserId: String,
        inviteeContact: String,
        channel: String,
        role: ListRole
    ): Result<InvitationEntity> = repo.createInvite(listId, inviterUserId, inviteeContact, channel, role)
}

class GetInvitesForListUseCase(private val repo: InvitationRepository) {
    operator fun invoke(listId: String): Flow<List<InvitationEntity>> = repo.getInvitesForList(listId)
}

class AcceptInviteUseCase(private val repo: InvitationRepository) {
    suspend operator fun invoke(token: String, userId: String): Result<Unit> = repo.accept(token, userId)
}

class MarkPresenceUseCase(private val repo: PresenceRepository) {
    suspend operator fun invoke(listId: String, userId: String): Result<Unit> = repo.markActive(listId, userId)
}

class GetPresenceForListUseCase(private val repo: PresenceRepository) {
    operator fun invoke(listId: String): Flow<List<PresenceEntity>> = repo.getForList(listId)
}
