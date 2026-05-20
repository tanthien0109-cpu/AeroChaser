package com.aerochaser.presentation.cloud

import android.content.Context
import android.content.ContextWrapper
import com.aerochaser.data.cloud.DriveFolder
import com.aerochaser.data.cloud.GoogleDrivePhotoSource
import com.aerochaser.data.cloud.GooglePhotosSource
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CloudImportViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Fakes
    private lateinit var fakeContext: Context
    private lateinit var fakeDriveSource: FakeDriveSource
    private lateinit var fakePhotosSource: FakePhotosSource
    private lateinit var fakeRepo: FakePhotoRepository

    private lateinit var viewModel: CloudImportViewModel

    class FakeContext : ContextWrapper(null)

    class FakeDriveSource(context: Context) : GoogleDrivePhotoSource(context) {
        var throwError = false
        override suspend fun listFolders(parentId: String): List<DriveFolder> {
            if (throwError) throw Exception("Network error")
            return listOf(DriveFolder("f1", "Folder 1"))
        }

        override suspend fun fetchPhotosInFolder(folderId: String): List<PhotoMetadata> {
            return emptyList()
        }

        override fun updateAuth(account: android.accounts.Account?) {}
    }

    class FakePhotosSource : GooglePhotosSource() {
        override fun updateAuth(token: String?) {}
    }

    class FakePhotoRepository : PhotoRepository {
        var existingUris = mutableSetOf<String>()
        var savedPhotos = mutableListOf<PhotoMetadata>()

        override suspend fun getLocalPhotos(): List<PhotoMetadata> = emptyList()
        override suspend fun getPhotoById(id: String): PhotoMetadata? = null

        override suspend fun savePhotoMetadata(metadata: PhotoMetadata) {
            savedPhotos.add(metadata)
            existingUris.add(metadata.localUri)
        }

        override suspend fun photoExistsByUri(uri: String): Boolean {
            return existingUris.contains(uri)
        }

        override suspend fun photoExists(metadata: PhotoMetadata): Boolean {
            return existingUris.contains(metadata.localUri) || savedPhotos.any { it.fileName == metadata.fileName && it.fileSizeBytes == metadata.fileSizeBytes }
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeContext = FakeContext()
        fakeDriveSource = FakeDriveSource(fakeContext)
        fakePhotosSource = FakePhotosSource()
        fakeRepo = FakePhotoRepository()
        viewModel = CloudImportViewModel(
            appContext = fakeContext,
            drivePhotoSource = fakeDriveSource,
            photosSource = fakePhotosSource,
            photoRepository = fakeRepo
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSetActiveTab() = runTest {
        assertEquals(CloudTab.DRIVE, viewModel.activeTab.value)
        viewModel.setActiveTab(CloudTab.PHOTOS)
        assertEquals(CloudTab.PHOTOS, viewModel.activeTab.value)
    }

    @Test
    fun testSignOutClearsState() = runTest {
        viewModel.onSignInFailed("Auth error")
        assertTrue(viewModel.authState.value is CloudAuthState.Error)

        viewModel.signOut()
        assertEquals(CloudAuthState.SignedOut, viewModel.authState.value)
        assertEquals(DriveState.Idle, viewModel.driveState.value)
        assertEquals(PhotosState.Idle, viewModel.photosState.value)
    }

    @Test
    fun testSignInFailedSetsErrorState() = runTest {
        viewModel.onSignInFailed("Connection timed out")
        assertEquals(CloudAuthState.Error("Connection timed out"), viewModel.authState.value)
    }
}


