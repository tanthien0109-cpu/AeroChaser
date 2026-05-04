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
}
