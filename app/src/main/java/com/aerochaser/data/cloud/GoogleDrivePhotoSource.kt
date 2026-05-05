package com.aerochaser.data.cloud

import android.content.Context
import com.aerochaser.domain.cloud.CloudPhotoSource
import com.aerochaser.domain.models.PhotoMetadata
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class GoogleDrivePhotoSource(private val context: Context) : CloudPhotoSource {
    override val sourceName: String = "Google Drive"

    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated

    private var driveService: Drive? = null

    init {
        checkAuth()
    }

    private fun checkAuth() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            setupDriveService(account.account)
        }
    }

    private fun setupDriveService(account: android.accounts.Account?) {
        if (account == null) return
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_READONLY)
        ).setSelectedAccount(account)

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("AeroChaser").build()

        _isAuthenticated.value = true
    }

    override suspend fun authenticate() {
        // Authentication is handled via the UI (CredentialManager)
        // Once signed in, checkAuth is called again
        checkAuth()
    }

    fun updateAuth(account: android.accounts.Account?) {
        setupDriveService(account)
    }

    override suspend fun fetchPhotos(): List<PhotoMetadata> = withContext(Dispatchers.IO) {
        val service = driveService ?: throw IllegalStateException("Not authenticated with Google Drive")
        val photos = mutableListOf<PhotoMetadata>()
        
        try {
            val result = service.files().list()
                .setQ("mimeType contains 'image/'")
                .setSpaces("drive")
                .setFields("nextPageToken, files(id, name, mimeType, createdTime, imageMediaMetadata)")
                .execute()

            result.files?.forEach { file ->
                val metadata = PhotoMetadata(
                    id = file.id,
                    localUri = "drive://${file.id}",
                    captureDateMs = file.createdTime?.value ?: System.currentTimeMillis(),
                    cameraModel = file.imageMediaMetadata?.cameraMake + " " + file.imageMediaMetadata?.cameraModel,
                    lensModel = file.imageMediaMetadata?.lens,
                    aperture = file.imageMediaMetadata?.aperture?.toString(),
                    shutterSpeed = file.imageMediaMetadata?.exposureTime?.toString(),
                    iso = file.imageMediaMetadata?.isoSpeed,
                    focalLength = file.imageMediaMetadata?.focalLength?.toString(),
                    gpsLat = file.imageMediaMetadata?.location?.latitude,
                    gpsLng = file.imageMediaMetadata?.location?.longitude
                )
                photos.add(metadata)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        photos
    }
}
