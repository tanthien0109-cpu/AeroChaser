package com.aerochaser.domain.cloud

import com.aerochaser.domain.models.PhotoMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Interface seed for future Cloud Integrations (Google Drive, Google Photos).
 */
interface CloudPhotoSource {
    val sourceName: String
    val isAuthenticated: Flow<Boolean>

    suspend fun authenticate()
    suspend fun fetchPhotos(): List<PhotoMetadata>
}
