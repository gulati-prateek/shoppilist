package com.shoppilist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppilist.shared.data.local.ListMemberEntity
import com.shoppilist.shared.data.local.ShoppingItemEntity
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.domain.AssigneeSummary
import com.shoppilist.shared.domain.GetAssignmentSummaryUseCase
import com.shoppilist.shared.domain.GetListItemsUseCase
import com.shoppilist.shared.domain.GetListMembersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WhosGettingWhatUiState(
    val byAssignee: List<AssigneeSummary> = emptyList(),
    val unassigned: List<ShoppingItemEntity> = emptyList(),
    val members: List<ListMemberEntity> = emptyList(),
    val userNames: Map<String, String> = emptyMap()
)

class AssignmentsViewModel(
    private val getListItemsUseCase: GetListItemsUseCase,
    private val getListMembersUseCase: GetListMembersUseCase,
    private val getAssignmentSummaryUseCase: GetAssignmentSummaryUseCase,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhosGettingWhatUiState())
    val uiState: StateFlow<WhosGettingWhatUiState> = _uiState

    fun load(listId: String) {
        viewModelScope.launch {
            getListMembersUseCase(listId).collect { members ->
                val names = mutableMapOf<String, String>()
                members.forEach { member ->
                    userDao.getUserOnce(member.userId)?.let { names[member.userId] = it.fullName }
                }
                _uiState.value = _uiState.value.copy(members = members, userNames = names)
            }
        }
        viewModelScope.launch {
            getListItemsUseCase(listId).collect { items ->
                val (byAssignee, unassigned) = getAssignmentSummaryUseCase(items)
                _uiState.value = _uiState.value.copy(byAssignee = byAssignee, unassigned = unassigned)
            }
        }
    }
}
