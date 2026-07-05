package com.shoppilist.data.repository

import com.shoppilist.shared.data.local.InvitationDao
import com.shoppilist.shared.data.local.InvitationEntity
import com.shoppilist.shared.data.local.ListMemberDao
import com.shoppilist.shared.data.local.ListRole
import com.shoppilist.shared.data.local.ShoppingItemDao
import com.shoppilist.shared.data.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RoomListMemberRepositoryTest {

    @Mock private lateinit var listMemberDao: ListMemberDao
    @Mock private lateinit var shoppingItemDao: ShoppingItemDao
    private lateinit var repo: RoomListMemberRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repo = RoomListMemberRepository(listMemberDao, shoppingItemDao)
    }

    @Test
    fun `removing a member unassigns their items in that list before removing them`() = runBlocking {
        val result = repo.removeMember("list1", "bob")

        assertTrue(result.isSuccess)
        verify(shoppingItemDao).unassignAllForUserInList("list1", "bob")
        verify(listMemberDao).remove("list1", "bob")
    }
}

class RoomInvitationRepositoryTest {

    @Mock private lateinit var invitationDao: InvitationDao
    @Mock private lateinit var listMemberRepository: ListMemberRepository
    private lateinit var repo: RoomInvitationRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        repo = RoomInvitationRepository(invitationDao, listMemberRepository)
    }

    @Test
    fun `accepting an invite adds the user as a list member with the invited role and marks it accepted`() = runBlocking {
        val invite = InvitationEntity(
            inviteId = "inv1",
            listId = "list1",
            inviterUserId = "alice",
            inviteeContact = "bob@example.com",
            channel = "email",
            role = ListRole.VIEWER
        )
        whenever(invitationDao.findByToken("tok1")).thenReturn(invite)

        val result = repo.accept("tok1", "bob")

        assertTrue(result.isSuccess)
        verify(listMemberRepository).addMember("list1", "bob", ListRole.VIEWER)
        verify(invitationDao).updateStatus("inv1", "ACCEPTED")
    }

    @Test
    fun `accepting an unknown token fails without touching membership`() = runBlocking {
        whenever(invitationDao.findByToken("bad")).thenReturn(null)

        val result = repo.accept("bad", "bob")

        assertTrue(result.isFailure)
        org.mockito.kotlin.verifyNoInteractions(listMemberRepository)
    }
}
