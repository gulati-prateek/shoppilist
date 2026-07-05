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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class InviteUiState(
    val members: List<ListMemberEntity> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val lastInviteLink: String? = null,
    val error: String? = null
)

class InviteViewModel(
    private val getListMembersUseCase: GetListMembersUseCase,
    private val createInviteUseCase: CreateInviteUseCase,
    private val removeListMemberUseCase: RemoveListMemberUseCase,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
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
                // No real SMS/email delivery in this project (no backend) — surface the join link directly.
                _uiState.value = _uiState.value.copy(lastInviteLink = "shoppilist.app/join/${invite.token}", error = null)
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message)
            }
        }
    }

    fun removeMember(listId: String, userId: String) {
        viewModelScope.launch { removeListMemberUseCase(listId, userId) }
    }
}
