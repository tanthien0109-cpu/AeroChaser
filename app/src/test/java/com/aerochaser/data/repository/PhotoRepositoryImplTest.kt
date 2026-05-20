package com.aerochaser.data.repository

import com.aerochaser.data.local.dao.PhotoDao
import com.aerochaser.data.local.entity.ExifDataEntity
import com.aerochaser.data.local.entity.PhotoEntity
import com.aerochaser.data.local.entity.PhotoWithExif
import com.aerochaser.domain.models.PhotoMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoRepositoryImplTest {

    private class FakePhotoDao : PhotoDao() {
        val photos = mutableMapOf<String, PhotoEntity>()
        val exifs = mutableMapOf<String, ExifDataEntity>()

        override suspend fun insertPhoto(photo: PhotoEntity) {
            photos[photo.id] = photo
        }

        override suspend fun insertExifData(exifData: ExifDataEntity) {
            exifs[exifData.photoId] = exifData
        }

        override suspend fun insertPhotoWithExif(photo: PhotoEntity, exifData: ExifDataEntity?) {
            insertPhoto(photo)
            if (exifData != null) {
                insertExifData(exifData)
            }
        }

        override fun getAllPhotos(): Flow<List<PhotoWithExif>> {
            return flowOf(
                photos.values.map { photo ->
                    PhotoWithExif(photo, exifs[photo.id])
                }
            )
        }

        override suspend fun getAllPhotosSync(): List<PhotoWithExif> {
            return photos.values.map { photo ->
                PhotoWithExif(photo, exifs[photo.id])
            }
        }

        override suspend fun getPhotoById(id: String): PhotoWithExif? {
            val photo = photos[id] ?: return null
            return PhotoWithExif(photo, exifs[id])
        }

        override suspend fun getPhotoIdByUri(uri: String): String? {
            return photos.values.find { it.localUri == uri }?.id
        }

        override suspend fun getPhotoIdByMetadata(fileName: String, sizeBytes: Long): String? {
            return photos.values.find { it.fileName == fileName && it.fileSizeBytes == sizeBytes }?.id
        }
    }

    private lateinit var fakeDao: FakePhotoDao
    private lateinit var repository: PhotoRepositoryImpl

    @Before
    fun setup() {
        fakeDao = FakePhotoDao()
        repository = PhotoRepositoryImpl(fakeDao)
    }

    @Test
    fun `photoExistsByUri works for cloud URIs`() = runTest {
        val cloudUri = "https://lh3.googleusercontent.com/a/ACg8ocKw..."
        
        assertFalse(repository.photoExistsByUri(cloudUri))

        fakeDao.insertPhoto(
            PhotoEntity(
                id = "1",
                localUri = cloudUri,
                captureDateMs = 12345L,
                importedAtMs = 67890L
            )
        )

        assertTrue(repository.photoExistsByUri(cloudUri))
    }

    @Test
    fun `savePhotoMetadata saves and retrieves cloud photos correctly`() = runTest {
        val metadata = PhotoMetadata(
            id = "cloud_photo_1",
            localUri = "https://photos.app.goo.gl/xyz",
            captureDateMs = 1000L,
            cameraModel = "CloudCam",
            lensModel = "CloudLens",
            aperture = "f/2.8",
            shutterSpeed = "1/1000",
            iso = 100,
            focalLength = "50mm",
            gpsLat = 10.0,
            gpsLng = 20.0,
            fileName = "IMG_0001.JPG",
            fileSizeBytes = 1024L,
            modifiedDateMs = 2000L,
            thumbnailUrl = "https://lh3.googleusercontent.com/xyz"
        )

        repository.savePhotoMetadata(metadata)

        val retrieved = repository.getPhotoById("cloud_photo_1")
        
        assertEquals(metadata.id, retrieved?.id)
        assertEquals(metadata.localUri, retrieved?.localUri)
        assertEquals(metadata.fileName, retrieved?.fileName)
        assertEquals(metadata.fileSizeBytes, retrieved?.fileSizeBytes)
        assertEquals(metadata.modifiedDateMs, retrieved?.modifiedDateMs)
        assertEquals(metadata.thumbnailUrl, retrieved?.thumbnailUrl)
    }
}
