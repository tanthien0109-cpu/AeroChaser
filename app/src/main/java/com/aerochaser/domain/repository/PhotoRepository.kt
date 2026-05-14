package com.aerochaser.domain.repository

import com.aerochaser.domain.models.PhotoMetadata

/**
 * Repository interface seed for photo persistence.
 * Implementations provide local (Room) or remote (Cloud) storage.
 * Directory scanning is handled by ScanDirectoryUseCase composing FileIO + ExifParser.
 */
interface PhotoRepository {
    suspend fun getLocalPhotos(): List<PhotoMetadata>
    suspend fun getPhotoById(id: String): PhotoMetadata?
    suspend fun savePhotoMetadata(metadata: PhotoMetadata)
    suspend fun photoExistsByUri(uri: String): Boolean
    /**
     * Checks if a photo already exists in the repository.
     * 
     * To prevent duplicates across different cloud providers (e.g., the same photo in both
     * Google Drive and Google Photos), this method performs a two-stage check:
     * 1. Exact match on [PhotoMetadata.localUri].
     * 2. Heuristic match on [PhotoMetadata.fileName] and [PhotoMetadata.fileSizeBytes].
     */
    suspend fun photoExists(metadata: PhotoMetadata): Boolean
}
