package com.shoppilist.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.ListMemberEntity
import com.shoppilist.shared.data.local.ListRole
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.domain.CreateInviteUseCase
import com.shoppilist.shared.domain.GetListMembersUseCase
import com.shoppilist.shared.domain.RemoveListMemberUseCase
import com.shoppilist.shared.sync.CollaborationSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InviteUiState(
    val members: List<ListMemberEntity> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    /** Contact of the most recently created invite — drives the "invite sent" confirmation row. */
    val lastInvitedContact: String? = null,
    /** One-shot signal: a just-created invite the UI should hand to the SMS/email composer. */
    val outgoingInvite: OutgoingInvite? = null,
    val error: String? = null
)

/** What the platform composer needs to pre-fill the invite message. No web link: the invite is
 *  delivered in-app via Firestore keyed by the invitee's contact, and there is no hosted domain
 *  yet — a `shoppilist.app/join/...` URL would just 404 (user-reported). */
data class OutgoingInvite(val contact: String, val channel: String)

class InviteViewModel(
    private val getListMembersUseCase: GetListMembersUseCase,
    private val createInviteUseCase: CreateInviteUseCase,
    private val removeListMemberUseCase: RemoveListMemberUseCase,
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val collaborationSync: CollaborationSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteUiState())
    val uiState: StateFlow<InviteUiState> = _uiState

    fun loadMembers(listId: String) {
        viewModelScope.launch {
            getListMembersUseCase(listId).collect { members ->
                val names = mutableMapOf<String, String>()
                members.forEach { m -> userDao.getUserOnce(m.userId)?.let { names[m.userId] = it.fullName } }
                _uiState.value = _uiState.value.copy(members = members, memberNames = names)
            }
        }
    }

    fun sendInvite(listId: String, contact: String, channel: String, role: ListRole) {
        viewModelScope.launch {
            val result = createInviteUseCase(listId, sessionManager.requireUserId(), contact, channel, role)
            result.onSuccess { invite ->
                // Phase 4: push the list + the invite to Firestore so the recipient's device sees it.
                collaborationSync.shareAndInvite(invite)
                _uiState.value = _uiState.value.copy(
                    lastInvitedContact = contact.trim(),
                    // Hand off to the platform SMS/email composer (consumed by the screen).
                    outgoingInvite = OutgoingInvite(contact = contact.trim(), channel = channel),
                    error = null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message)
            }
        }
    }

    /** The screen handled (or failed to handle) the composer hand-off — clear the one-shot. */
    fun consumeOutgoingInvite() {
        _uiState.value = _uiState.value.copy(outgoingInvite = null)
    }

    fun removeMember(listId: String, userId: String) {
        viewModelScope.launch { removeListMemberUseCase(listId, userId) }
    }
}
