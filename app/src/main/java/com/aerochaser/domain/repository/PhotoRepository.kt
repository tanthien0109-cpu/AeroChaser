package com.aerochaser.domain.repository

import com.aerochaser.domain.models.PhotoMetadata

/**
 * Explicit repository interface "seed". 
 * This defines the contract for fetching and managing photos,
 * independent of whether they come from a local SQLite DB or Google Drive.
 */
interface PhotoRepository {
    suspend fun getLocalPhotos(): List<PhotoMetadata>
    suspend fun savePhotoMetadata(metadata: PhotoMetadata)
    suspend fun scanLocalDirectoryForPhotos(directoryUri: String): Int
}
