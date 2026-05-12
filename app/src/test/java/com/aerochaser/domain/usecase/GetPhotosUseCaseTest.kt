package com.aerochaser.domain.usecase

import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetPhotosUseCaseTest {

    private class FakePhotoRepository(private val photos: List<PhotoMetadata>) : PhotoRepository {
        override suspend fun getLocalPhotos(): List<PhotoMetadata> = photos
        override suspend fun getPhotoById(id: String): PhotoMetadata? = photos.find { it.id == id }
        override suspend fun savePhotoMetadata(metadata: PhotoMetadata) {}
        override suspend fun photoExistsByUri(uri: String): Boolean = photos.any { it.localUri == uri }
    }

    private fun makePhoto(id: String, dateMs: Long) = PhotoMetadata(
        id = id,
        localUri = "content://fake/$id.jpg",
        captureDateMs = dateMs,
        cameraModel = null,
        lensModel = null,
        aperture = null,
        shutterSpeed = null,
        iso = null,
        focalLength = null,
        gpsLat = null,
        gpsLng = null
    )

    @Test
    fun `returns photos sorted by date descending`() = runTest {
        val photos = listOf(
            makePhoto("old", 1000L),
            makePhoto("new", 3000L),
            makePhoto("mid", 2000L)
        )
        val useCase = GetPhotosUseCase(FakePhotoRepository(photos))
        val result = useCase()

        assertEquals(3, result.size)
        assertEquals("new", result[0].id)
        assertEquals("mid", result[1].id)
        assertEquals("old", result[2].id)
    }

    @Test
    fun `returns empty list when no photos`() = runTest {
        val useCase = GetPhotosUseCase(FakePhotoRepository(emptyList()))
        val result = useCase()
        assertTrue(result.isEmpty())
    }
}
