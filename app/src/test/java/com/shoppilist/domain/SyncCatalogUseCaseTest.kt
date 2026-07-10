package com.shoppilist.domain

import com.shoppilist.shared.backend.CatalogBackend
import com.shoppilist.shared.backend.RemoteCatalogItem
import com.shoppilist.shared.currentTimeMillis
import com.shoppilist.shared.data.local.GlobalItemDao
import com.shoppilist.shared.data.session.SessionManager
import com.shoppilist.shared.domain.CatalogRegion
import com.shoppilist.shared.domain.SyncCatalogUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SyncCatalogUseCaseTest {

    @Mock private lateinit var globalItemDao: GlobalItemDao
    @Mock private lateinit var catalogBackend: CatalogBackend
    @Mock private lateinit var sessionManager: SessionManager

    private lateinit var useCase: SyncCatalogUseCase

    private val remoteItems = listOf(
        RemoteCatalogItem(id = "basmati-rice", name = "basmati rice", categoryId = "canned_packaged")
    )

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        whenever(catalogBackend.isAvailable).thenReturn(true)
        useCase = SyncCatalogUseCase(globalItemDao, catalogBackend, sessionManager)
    }

    @Test
    fun `a stale region is refreshed - replace rows and stamp the sync time`() = runBlocking<Unit> {
        whenever(sessionManager.catalogSyncedAt("IN")).thenReturn(null)
        whenever(catalogBackend.fetchCatalog("IN")).thenReturn(Result.success(remoteItems))

        useCase("IN")

        verify(globalItemDao).deleteByRegion("IN")
        verify(globalItemDao).insertAll(argThat { single().id == "IN-basmati-rice" && single().region == "IN" })
        verify(sessionManager).setCatalogSyncedAt(eq("IN"), any())
    }

    @Test
    fun `a fresh cache is not refetched`() = runBlocking<Unit> {
        whenever(sessionManager.catalogSyncedAt("IN")).thenReturn(currentTimeMillis() - 60_000)

        useCase("IN")

        verify(catalogBackend, never()).fetchCatalog(any())
        verify(globalItemDao, never()).deleteByRegion(any())
    }

    @Test
    fun `force bypasses the staleness window`() = runBlocking<Unit> {
        whenever(sessionManager.catalogSyncedAt("IN")).thenReturn(currentTimeMillis() - 60_000)
        whenever(catalogBackend.fetchCatalog("IN")).thenReturn(Result.success(remoteItems))

        useCase("IN", force = true)

        verify(globalItemDao).deleteByRegion("IN")
    }

    @Test
    fun `a failed fetch leaves the existing cache untouched`() = runBlocking<Unit> {
        whenever(sessionManager.catalogSyncedAt("US")).thenReturn(null)
        whenever(catalogBackend.fetchCatalog("US")).thenReturn(Result.failure(IllegalStateException("offline")))

        useCase("US")

        verify(globalItemDao, never()).deleteByRegion(any())
        verify(sessionManager, never()).setCatalogSyncedAt(any(), any())
    }

    @Test
    fun `an empty remote catalog never wipes the cache`() = runBlocking<Unit> {
        whenever(sessionManager.catalogSyncedAt("EU")).thenReturn(null)
        whenever(catalogBackend.fetchCatalog("EU")).thenReturn(Result.success(emptyList()))

        useCase("EU")

        verify(globalItemDao, never()).deleteByRegion(any())
    }

    @Test
    fun `global and unknown regions are never synced`() = runBlocking<Unit> {
        useCase(CatalogRegion.GLOBAL)
        useCase("XX")

        verify(catalogBackend, never()).fetchCatalog(anyOrNull())
    }
}
