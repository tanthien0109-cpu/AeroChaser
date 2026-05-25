package com.aerochaser.data.cloud

import android.accounts.Account
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GoogleDrivePhotoSourceTest {

    private class FakeContext : ContextWrapper(null)

    private class FakeGoogleDrivePhotoSource(context: Context) : GoogleDrivePhotoSource(context) {
        var lastAccountPassed: Account? = null

        override fun updateAuth(account: Account?) {
            lastAccountPassed = account
            _isAuthenticated.value = account != null
        }
    }

    private lateinit var driveSource: FakeGoogleDrivePhotoSource
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = FakeContext()
        driveSource = FakeGoogleDrivePhotoSource(mockContext)
    }

    @Test
    fun testDefaultState() = runTest {
        assertEquals("Google Drive", driveSource.sourceName)
        assertFalse(driveSource.isAuthenticated.first())
    }

    @Test
    fun testUpdateAuthWithNullAccount() = runTest {
        driveSource.updateAuth(null)
        assertFalse(driveSource.isAuthenticated.first())
        assertEquals(null, driveSource.lastAccountPassed)
    }

    @Test
    fun testUpdateAuthWithValidAccount() = runTest {
        val account = Account("test@gmail.com", "com.google")
        driveSource.updateAuth(account)
        assertTrue(driveSource.isAuthenticated.first())
        assertTrue(driveSource.lastAccountPassed != null)
    }

    @Test
    fun testListFoldersWithoutAuthThrowsException() = runTest {
        val realSource = GoogleDrivePhotoSource(mockContext)
        try {
            realSource.listFolders()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Not authenticated with Google Drive", e.message)
        }
    }

    @Test
    fun testFetchPhotosWithoutAuthThrowsException() = runTest {
        val realSource = GoogleDrivePhotoSource(mockContext)
        try {
            realSource.fetchPhotos()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Not authenticated with Google Drive", e.message)
        }
    }
}
