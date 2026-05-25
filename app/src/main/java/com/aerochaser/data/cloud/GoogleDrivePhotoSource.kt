package com.aerochaser.data.cloud

import android.content.Context
import android.util.Log
import com.aerochaser.domain.cloud.CloudPhotoSource
import com.aerochaser.domain.models.PhotoMetadata
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/**
 * Google Drive integration for AeroChaser.
 *
 * Auth lifecycle:
 * - UI calls CredentialManager → receives Account → calls [updateAuth]
 * - [updateAuth] builds the Drive REST client
 * - [fetchPhotos] queries for image files
 * - [listFolders] lists top-level Drive folders for browsing
 */
open class GoogleDrivePhotoSource(private val context: Context) : CloudPhotoSource {

    companion object {
        private const val TAG = "GoogleDrivePhotoSource"
    }

    override val sourceName: String = "Google Drive"

    protected val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated

    @Volatile
    private var driveService: Drive? = null

    override suspend fun authenticate() {
        // Authentication is handled via the UI (CredentialManager).
        // Once signed in, the UI calls updateAuth() with the Account.
    }

    /**
     * Called by the UI after CredentialManager sign-in succeeds.
     * Builds the Drive REST client with DRIVE_READONLY scope.
     */
    open fun updateAuth(account: android.accounts.Account?) {
        if (account == null) {
            _isAuthenticated.value = false
            driveService = null
            return
        }

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_READONLY)
        ).setSelectedAccount(account)

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("AeroChaser").build()

        _isAuthenticated.value = true
        Log.d(TAG, "Drive service authenticated for: ${account.name}")
    }

    /**
     * Lists folders in the user's Drive under the given parent.
     * @param parentId The parent folder ID, or "root" for top-level folders.
     */
    open suspend fun listFolders(parentId: String = "root"): List<DriveFolder> = withContext(Dispatchers.IO) {
        val service = driveService ?: throw IllegalStateException("Not authenticated with Google Drive")

        try {
            val result = service.files().list()
                .setQ("mimeType = 'application/vnd.google-apps.folder' and '$parentId' in parents and trashed = false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .setOrderBy("name")
                .execute()

            result.files?.map { file ->
                DriveFolder(id = file.id, name = file.name)
            } ?: emptyList()
        } catch (e: UserRecoverableAuthIOException) {
            Log.e(TAG, "User must grant Drive permission", e)
            throw DriveAuthException("Google Drive access not granted. Please ensure you checked and granted the Google Drive permission on the sign-in consent screen.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list folders", e)
            throw e
        }
    }

    /**
     * Lists photos in a specific Drive folder.
     */
    open suspend fun fetchPhotosInFolder(folderId: String): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val service = driveService ?: throw IllegalStateException("Not authenticated with Google Drive")
        fetchPhotosWithQuery(service, "'$folderId' in parents and mimeType contains 'image/' and trashed = false")
    }

    override suspend fun fetchPhotos(): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val service = driveService ?: throw IllegalStateException("Not authenticated with Google Drive")
        fetchPhotosWithQuery(service, "mimeType contains 'image/' and trashed = false")
    }

    private fun fetchPhotosWithQuery(service: Drive, query: String): List<PhotoMetadata> {
        val photos = mutableListOf<PhotoMetadata>()

        try {
            var pageToken: String? = null
            do {
                val request = service.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, mimeType, size, modifiedTime, createdTime, thumbnailLink, imageMediaMetadata)")
                    .setPageSize(100)

                if (pageToken != null) request.pageToken = pageToken

                val result = request.execute()

                result.files?.forEach { file ->
                    val imgMeta = file.imageMediaMetadata

                    // Build camera model from make + model, handling nulls properly
                    val cameraModel = listOfNotNull(
                        imgMeta?.cameraMake,
                        imgMeta?.cameraModel
                    ).joinToString(" ").ifBlank { null }

                    val metadata = PhotoMetadata(
                        id = file.id,
                        localUri = "drive://${file.id}",
                        captureDateMs = file.createdTime?.value ?: System.currentTimeMillis(),
                        cameraModel = cameraModel,
                        lensModel = imgMeta?.lens,
                        aperture = imgMeta?.aperture?.toString(),
                        shutterSpeed = imgMeta?.exposureTime?.toString(),
                        iso = imgMeta?.isoSpeed,
                        focalLength = imgMeta?.focalLength?.toString(),
                        gpsLat = imgMeta?.location?.latitude,
                        gpsLng = imgMeta?.location?.longitude,
                        fileName = file.name,
                        fileSizeBytes = file.getSize()?.toLong(),
                        modifiedDateMs = file.modifiedTime?.value,
                        thumbnailUrl = file.thumbnailLink
                    )
                    photos.add(metadata)
                }

                pageToken = result.nextPageToken
            } while (pageToken != null)
        } catch (e: UserRecoverableAuthIOException) {
            Log.e(TAG, "User must grant Drive permission", e)
            throw DriveAuthException("Google Drive access not granted. Please ensure you checked and granted the Google Drive permission on the sign-in consent screen.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch photos: ${e.message}", e)
            throw e
        }

        return photos
    }
}

/**
 * Minimal representation of a Google Drive folder for the browse UI.
 */
data class DriveFolder(
    val id: String,
    val name: String
)

/**
 * Thrown when the user has not granted Drive OAuth permission.
 * The UI should prompt re-authentication with the correct scopes.
 */
class DriveAuthException(message: String, cause: Throwable? = null) : Exception(message, cause)
