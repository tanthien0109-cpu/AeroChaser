package com.aerochaser.presentation.cloud

import android.util.Log
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.data.cloud.DriveFolder
import com.aerochaser.data.cloud.GoogleDrivePhotoSource
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CloudAuthState {
    data object SignedOut : CloudAuthState
    data object SigningIn : CloudAuthState
    data class SignedIn(val email: String) : CloudAuthState
    data class Error(val message: String) : CloudAuthState
}

sealed interface ImportState {
    data object Idle : ImportState
    data object Loading : ImportState
    data class FolderList(val folders: List<DriveFolder>) : ImportState
    data class PhotoList(val folderId: String, val folderName: String, val photos: List<PhotoMetadata>) : ImportState
    data class Importing(val current: Int, val total: Int, val skipped: Int) : ImportState
    data class Complete(val imported: Int, val skipped: Int) : ImportState
    data class Error(val message: String) : ImportState
}

class CloudImportViewModel(
    private val drivePhotoSource: GoogleDrivePhotoSource,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CloudImportVM"
    }

    private val _authState = MutableStateFlow<CloudAuthState>(CloudAuthState.SignedOut)
    val authState: StateFlow<CloudAuthState> = _authState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    /**
     * Builds the CredentialManager request for Google Sign-In.
     */
    fun buildSignInRequest(webClientId: String): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    /**
     * Called after successful CredentialManager sign-in.
     * Passes the Account to DrivePhotoSource and updates auth state.
     */
    fun onSignInSuccess(account: android.accounts.Account, email: String) {
        drivePhotoSource.updateAuth(account)
        _authState.value = CloudAuthState.SignedIn(email)
        Log.d(TAG, "Signed in as: $email")
    }

    fun onSignInFailed(message: String) {
        _authState.value = CloudAuthState.Error(message)
        Log.e(TAG, "Sign-in failed: $message")
    }

    fun signOut() {
        drivePhotoSource.updateAuth(null)
        _authState.value = CloudAuthState.SignedOut
        _importState.value = ImportState.Idle
    }

    /**
     * Loads the list of top-level Drive folders.
     */
    fun loadFolders() {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val folders = drivePhotoSource.listFolders()
                _importState.value = ImportState.FolderList(folders)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load folders", e)
                _importState.value = ImportState.Error("Could not load Drive folders: ${e.message}")
            }
        }
    }

    /**
     * Loads photos from a specific Drive folder for preview.
     */
    fun loadPhotosInFolder(folderId: String, folderName: String) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val photos = drivePhotoSource.fetchPhotosInFolder(folderId)
                _importState.value = ImportState.PhotoList(folderId, folderName, photos)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to list photos in folder: $folderId", e)
                _importState.value = ImportState.Error("Could not list photos in '$folderName': ${e.message}")
            }
        }
    }

    /**
     * Imports photos from the current preview list, skipping duplicates.
     */
    fun importPhotos(photos: List<PhotoMetadata>) {
        viewModelScope.launch {
            val total = photos.size
            var imported = 0
            var skipped = 0

            _importState.value = ImportState.Importing(0, total, 0)

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

                _importState.value = ImportState.Importing(index + 1, total, skipped)
            }

            _importState.value = ImportState.Complete(imported, skipped)
            Log.d(TAG, "Import complete: $imported imported, $skipped skipped")
        }
    }

    fun goBack() {
        when (_importState.value) {
            is ImportState.PhotoList -> loadFolders()
            is ImportState.Complete -> loadFolders()
            is ImportState.Error -> loadFolders()
            else -> _importState.value = ImportState.Idle
        }
    }
}
