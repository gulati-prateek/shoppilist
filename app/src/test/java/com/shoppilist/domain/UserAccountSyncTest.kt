package com.shoppilist.domain

import com.shoppilist.shared.auth.AuthUser
import com.shoppilist.shared.backend.ProfileBackend
import com.shoppilist.shared.backend.RemoteProfile
import com.shoppilist.shared.data.local.UserDao
import com.shoppilist.shared.data.local.UserEntity
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.domain.UserAccountSync
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Pins the profile-setup gate: firstName on the returned row decides who sees the form. */
class UserAccountSyncTest {

    @Mock private lateinit var userDao: UserDao
    @Mock private lateinit var sessionManager: SessionManager
    @Mock private lateinit var profileBackend: ProfileBackend

    private lateinit var sync: UserAccountSync

    private val phoneUser = AuthUser(uid = "uid1", email = null, emailVerified = false, phoneNumber = "+911234567890")

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sync = UserAccountSync(userDao, sessionManager, profileBackend)
    }

    @Test
    fun `a brand-new phone user has no first name - profile form required`() = runBlocking {
        whenever(userDao.getUserOnce("uid1")).thenReturn(null)
        whenever(profileBackend.fetchProfile("uid1")).thenReturn(null)

        val local = sync.ensureLocalUser(phoneUser)

        assertNull(local.firstName)
        assertEquals("+911234567890", local.fullName)
        verify(sessionManager).setCurrentUser("uid1")
        verify(userDao).upsert(argThat { userId == "uid1" })
    }

    @Test
    fun `a returning user on a fresh install is restored from the cloud mirror`() = runBlocking {
        whenever(userDao.getUserOnce("uid1")).thenReturn(null)
        whenever(profileBackend.fetchProfile("uid1")).thenReturn(
            RemoteProfile(uid = "uid1", firstName = "Prateek", lastName = "G", countryCode = "IN")
        )

        val local = sync.ensureLocalUser(phoneUser)

        assertEquals("Prateek", local.firstName)
        assertEquals("Prateek G", local.fullName)
        assertEquals("IN", local.countryCode)
    }

    @Test
    fun `an existing local profile is kept and not overwritten with blanks`() = runBlocking {
        val existing = UserEntity(
            userId = "uid1", fullName = "Prateek G", phone = "+911234567890", email = null,
            country = null, state = null, city = null, pincode = null, profileImageUrl = null,
            firstName = "Prateek", lastName = "G", address = "42 Some Street"
        )
        whenever(userDao.getUserOnce("uid1")).thenReturn(existing)

        val local = sync.ensureLocalUser(phoneUser, fullName = null)

        assertEquals("Prateek", local.firstName)
        assertEquals("42 Some Street", local.address)
        assertEquals("Prateek G", local.fullName)
    }
}
