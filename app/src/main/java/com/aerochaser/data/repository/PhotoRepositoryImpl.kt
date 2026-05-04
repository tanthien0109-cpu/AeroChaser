package com.aerochaser.data.repository

import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository

class PhotoRepositoryImpl : PhotoRepository {
    override suspend fun getLocalPhotos(): List<PhotoMetadata> {
        // TODO: Implement with Room database in Phase 2
        return emptyList()
    }

    override suspend fun savePhotoMetadata(metadata: PhotoMetadata) {
        // TODO: Implement database insertion
    }

    override suspend fun scanLocalDirectoryForPhotos(directoryUri: String): Int {
        // TODO: Implement directory scanning and EXIF parsing
        return 0
    }
}
