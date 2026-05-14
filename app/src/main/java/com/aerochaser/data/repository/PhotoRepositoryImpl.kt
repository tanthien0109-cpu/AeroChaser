package com.aerochaser.data.repository

import com.aerochaser.data.local.dao.PhotoDao
import com.aerochaser.data.local.entity.ExifDataEntity
import com.aerochaser.data.local.entity.PhotoEntity
import com.aerochaser.data.local.entity.PhotoWithExif
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import com.aerochaser.domain.usecase.HardwareClassifier

class PhotoRepositoryImpl(
    private val photoDao: PhotoDao
) : PhotoRepository {

    override suspend fun getLocalPhotos(): List<PhotoMetadata> {
        return photoDao.getAllPhotosSync().map { it.toDomain() }
    }

    override suspend fun getPhotoById(id: String): PhotoMetadata? {
        return photoDao.getPhotoById(id)?.toDomain()
    }

    override suspend fun savePhotoMetadata(metadata: PhotoMetadata) {
        val photoEntity = PhotoEntity(
            id = metadata.id,
            localUri = metadata.localUri,
            captureDateMs = metadata.captureDateMs,
            importedAtMs = System.currentTimeMillis(),
            fileName = metadata.fileName,
            fileSizeBytes = metadata.fileSizeBytes,
            modifiedDateMs = metadata.modifiedDateMs,
            thumbnailUrl = metadata.thumbnailUrl
        )
        val exifEntity = ExifDataEntity(
            photoId = metadata.id,
            cameraModel = metadata.cameraModel,
            lensModel = metadata.lensModel,
            aperture = metadata.aperture,
            shutterSpeed = metadata.shutterSpeed,
            iso = metadata.iso,
            focalLength = metadata.focalLength,
            gpsLat = metadata.gpsLat,
            gpsLng = metadata.gpsLng
        )
        photoDao.insertPhotoWithExif(photoEntity, exifEntity)
    }

    private fun PhotoWithExif.toDomain(): PhotoMetadata {
        return PhotoMetadata(
            id = photo.id,
            localUri = photo.localUri,
            captureDateMs = photo.captureDateMs,
            cameraModel = exifData?.cameraModel,
            lensModel = exifData?.lensModel,
            aperture = exifData?.aperture,
            shutterSpeed = exifData?.shutterSpeed,
            iso = exifData?.iso,
            focalLength = exifData?.focalLength,
            gpsLat = exifData?.gpsLat,
            gpsLng = exifData?.gpsLng,
            systemType = HardwareClassifier.classify(exifData?.cameraModel, exifData?.lensModel),
            fileName = photo.fileName,
            fileSizeBytes = photo.fileSizeBytes,
            modifiedDateMs = photo.modifiedDateMs,
            thumbnailUrl = photo.thumbnailUrl
        )
    }

    override suspend fun photoExistsByUri(uri: String): Boolean {
        return photoDao.getPhotoIdByUri(uri) != null
    }

    override suspend fun photoExists(metadata: PhotoMetadata): Boolean {
        // First check exact URI match (local file or same cloud source)
        if (photoDao.getPhotoIdByUri(metadata.localUri) != null) {
            return true
        }
        
        // If it's a cloud photo, checking by URI might fail across providers (Drive vs Photos)
        // Fallback to checking fileName and fileSizeBytes if they exist
        if (!metadata.fileName.isNullOrEmpty() && metadata.fileSizeBytes != null) {
            if (photoDao.getPhotoIdByMetadata(metadata.fileName, metadata.fileSizeBytes) != null) {
                return true
            }
        }
        
        return false
    }
}
