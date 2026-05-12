package com.aerochaser.presentation.cloud

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.data.cloud.DriveAuthException
import com.aerochaser.data.cloud.DriveFolder
import com.aerochaser.data.cloud.GoogleDrivePhotoSource
import com.aerochaser.data.cloud.GooglePhotosSource
import com.aerochaser.data.cloud.PhotosAlbum
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CloudAuthState {
    data object SignedOut : CloudAuthState
    data object SigningIn : CloudAuthState
    data class SignedIn(val email: String) : CloudAuthState
    data class Error(val message: String) : CloudAuthState
}

/** Which cloud service tab is active. */
enum class CloudTab { DRIVE, PHOTOS }

/** Represents a breadcrumb entry in the Drive folder navigation stack. */
data class FolderBreadcrumb(val id: String, val name: String)

sealed interface DriveState {
    data object Idle : DriveState
    data object Loading : DriveState
    data class FolderList(
        val folders: List<DriveFolder>,
        val breadcrumbs: List<FolderBreadcrumb>
    ) : DriveState
    data class PhotoList(
        val folderId: String,
        val folderName: String,
        val photos: List<PhotoMetadata>,
        val breadcrumbs: List<FolderBreadcrumb>
    ) : DriveState
    data class Importing(val current: Int, val total: Int, val skipped: Int) : DriveState
    data class Complete(val imported: Int, val skipped: Int) : DriveState
    data class Error(val message: String, val isAuthError: Boolean = false) : DriveState
}

sealed interface PhotosState {
    data object Idle : PhotosState
    data object Loading : PhotosState
    data class AlbumList(val albums: List<PhotosAlbum>) : PhotosState
    data class PhotoGrid(
        val albumId: String,
        val albumTitle: String,
        val photos: List<PhotoMetadata>
    ) : PhotosState
    data class Importing(val current: Int, val total: Int, val skipped: Int) : PhotosState
    data class Complete(val imported: Int, val skipped: Int) : PhotosState
    data class Error(val message: String) : PhotosState
}

class CloudImportViewModel(
    private val appContext: Context,
    private val drivePhotoSource: GoogleDrivePhotoSource,
    private val photosSource: GooglePhotosSource,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CloudImportVM"
    }

    private val _authState = MutableStateFlow<CloudAuthState>(CloudAuthState.SignedOut)
    val authState: StateFlow<CloudAuthState> = _authState.asStateFlow()

    private val _activeTab = MutableStateFlow(CloudTab.DRIVE)
    val activeTab: StateFlow<CloudTab> = _activeTab.asStateFlow()

    private val _driveState = MutableStateFlow<DriveState>(DriveState.Idle)
    val driveState: StateFlow<DriveState> = _driveState.asStateFlow()

    private val _photosState = MutableStateFlow<PhotosState>(PhotosState.Idle)
    val photosState: StateFlow<PhotosState> = _photosState.asStateFlow()

    // Navigation stack for Drive folder browsing
    private val folderStack = mutableListOf<FolderBreadcrumb>()

    fun setActiveTab(tab: CloudTab) {
        _activeTab.value = tab
    }

    // ── Auth ──────────────────────────────────────────────────

    fun onSignInSuccess(account: android.accounts.Account, email: String) {
        drivePhotoSource.updateAuth(account)
        _authState.value = CloudAuthState.SignedIn(email)
        Log.d(TAG, "Signed in as: $email")
        // Auto-load Drive folders
        loadDriveFolders()
        // Fetch Photos access token in background, then load albums
        viewModelScope.launch {
            try {
                val token = withContext(Dispatchers.IO) {
                    GoogleAuthUtil.getToken(
                        appContext,
                        account,
                        "oauth2:https://www.googleapis.com/auth/photoslibrary.readonly"
                    )
                }
                photosSource.updateAuth(token)
                loadPhotosAlbums()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get Photos access token", e)
                _photosState.value = PhotosState.Error("Could not authenticate with Google Photos: ${e.message}")
            }
        }
    }

    fun onSignInFailed(message: String) {
        _authState.value = CloudAuthState.Error(message)
        Log.e(TAG, "Sign-in failed: $message")
    }

    fun signOut() {
        drivePhotoSource.updateAuth(null)
        photosSource.updateAuth(null)
        _authState.value = CloudAuthState.SignedOut
        _driveState.value = DriveState.Idle
        _photosState.value = PhotosState.Idle
        folderStack.clear()
    }

    // ── Drive ─────────────────────────────────────────────────

    fun loadDriveFolders(parentId: String = "root") {
        _driveState.value = DriveState.Loading
        viewModelScope.launch {
            try {
                // If navigating into a new folder, push to stack
                if (parentId != "root" || folderStack.isEmpty()) {
                    if (parentId == "root") {
                        folderStack.clear()
                        folderStack.add(FolderBreadcrumb("root", "My Drive"))
                    }
                }
                val folders = drivePhotoSource.listFolders(parentId)
                _driveState.value = DriveState.FolderList(
                    folders = folders,
                    breadcrumbs = folderStack.toList()
                )
            } catch (e: DriveAuthException) {
                Log.e(TAG, "Drive auth error", e)
                _driveState.value = DriveState.Error(e.message ?: "Drive authentication failed", isAuthError = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Drive folders", e)
                _driveState.value = DriveState.Error("Could not load Drive folders: ${e.message}")
            }
        }
    }

    fun navigateIntoFolder(folderId: String, folderName: String) {
        folderStack.add(FolderBreadcrumb(folderId, folderName))
        loadDriveFolders(folderId)
    }

    fun navigateToFolderBreadcrumb(index: Int) {
        if (index < 0 || index >= folderStack.size) return
        val target = folderStack[index]
        // Remove everything after this index
        while (folderStack.size > index + 1) {
            folderStack.removeAt(folderStack.size - 1)
        }
        loadDriveFolders(target.id)
    }

    fun loadDrivePhotosInFolder(folderId: String, folderName: String) {
        _driveState.value = DriveState.Loading
        viewModelScope.launch {
            try {
                val photos = drivePhotoSource.fetchPhotosInFolder(folderId)
                _driveState.value = DriveState.PhotoList(
                    folderId = folderId,
                    folderName = folderName,
                    photos = photos,
                    breadcrumbs = folderStack.toList()
                )
            } catch (e: DriveAuthException) {
                _driveState.value = DriveState.Error(e.message ?: "Drive authentication failed", isAuthError = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to list photos in folder: $folderId", e)
                _driveState.value = DriveState.Error("Could not list photos in '$folderName': ${e.message}")
            }
        }
    }

    fun importDrivePhotos(photos: List<PhotoMetadata>) {
        viewModelScope.launch {
            val total = photos.size
            var imported = 0
            var skipped = 0
            _driveState.value = DriveState.Importing(0, total, 0)

            for ((index, photo) in photos.withIndex()) {
                try {
                    val exists = photoRepository.photoExistsByUri(photo.localUri)
                    if (exists) {
                        skipped++
                    } else {
                        photoRepository.savePhotoMetadata(photo)
                        imported++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import photo: ${photo.id}", e)
                    skipped++
                }
                _driveState.value = DriveState.Importing(index + 1, total, skipped)
            }
            _driveState.value = DriveState.Complete(imported, skipped)
            Log.d(TAG, "Drive import complete: $imported imported, $skipped skipped")
        }
    }

    fun driveGoBack() {
        when (_driveState.value) {
            is DriveState.PhotoList -> {
                // Go back to folder list at current level
                val currentParent = if (folderStack.size > 1) folderStack[folderStack.size - 1] else folderStack.firstOrNull()
                if (currentParent != null) {
                    loadDriveFolders(currentParent.id)
                } else {
                    loadDriveFolders()
                }
            }
            is DriveState.Complete, is DriveState.Error -> {
                val currentParent = folderStack.lastOrNull()
                if (currentParent != null) {
                    loadDriveFolders(currentParent.id)
                } else {
                    loadDriveFolders()
                }
            }
            else -> _driveState.value = DriveState.Idle
        }
    }

    fun driveNavigateUp(): Boolean {
        if (folderStack.size <= 1) return false
        folderStack.removeAt(folderStack.size - 1)
        val parent = folderStack.last()
        loadDriveFolders(parent.id)
        return true
    }

    // ── Photos ────────────────────────────────────────────────

    fun loadPhotosAlbums() {
        _photosState.value = PhotosState.Loading
        viewModelScope.launch {
            try {
                val albums = photosSource.listAlbums()
                _photosState.value = PhotosState.AlbumList(albums)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Photos albums", e)
                _photosState.value = PhotosState.Error("Could not load albums: ${e.message}")
            }
        }
    }

    fun loadPhotosInAlbum(albumId: String, albumTitle: String) {
        _photosState.value = PhotosState.Loading
        viewModelScope.launch {
            try {
                val photos = photosSource.listPhotosInAlbum(albumId)
                _photosState.value = PhotosState.PhotoGrid(albumId, albumTitle, photos)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load photos in album: $albumId", e)
                _photosState.value = PhotosState.Error("Could not load photos from '$albumTitle': ${e.message}")
            }
        }
    }

    fun importPhotosFromAlbum(photos: List<PhotoMetadata>) {
        viewModelScope.launch {
            val total = photos.size
            var imported = 0
            var skipped = 0
            _photosState.value = PhotosState.Importing(0, total, 0)

            for ((index, photo) in photos.withIndex()) {
                try {
                    val exists = photoRepository.photoExistsByUri(photo.localUri)
                    if (exists) {
                        skipped++
                    } else {
                        photoRepository.savePhotoMetadata(photo)
                        imported++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import photo: ${photo.id}", e)
                    skipped++
                }
                _photosState.value = PhotosState.Importing(index + 1, total, skipped)
            }
            _photosState.value = PhotosState.Complete(imported, skipped)
            Log.d(TAG, "Photos import complete: $imported imported, $skipped skipped")
        }
    }

    fun photosGoBack() {
        when (_photosState.value) {
            is PhotosState.PhotoGrid -> loadPhotosAlbums()
            is PhotosState.Complete, is PhotosState.Error -> loadPhotosAlbums()
            else -> _photosState.value = PhotosState.Idle
        }
    }
}
