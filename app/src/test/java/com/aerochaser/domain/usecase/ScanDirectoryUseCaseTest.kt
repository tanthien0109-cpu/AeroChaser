package com.aerochaser.domain.usecase

import com.aerochaser.domain.exif.ExifParser
import com.aerochaser.domain.io.FileIO
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanDirectoryUseCaseTest {

    // Fake implementations for testing without Android dependencies

    private class FakeFileIO(private val files: List<String>) : FileIO {
        override suspend fun getFilesFromDirectory(directoryUri: String): List<String> = files
        override suspend fun fileExists(uri: String): Boolean = files.contains(uri)
    }

    private class FakeExifParser(private val shouldSucceed: Boolean) : ExifParser {
        override suspend fun parseExif(uri: String, photoId: String): PhotoMetadata? {
            if (!shouldSucceed) return null
            return PhotoMetadata(
                id = photoId,
                localUri = uri,
                captureDateMs = 1000L,
                cameraModel = "TestCam",
                lensModel = null,
                aperture = "2.8",
                shutterSpeed = "1/1000",
                iso = 200,
                focalLength = "50",
                gpsLat = null,
                gpsLng = null
            )
        }
    }

    private class FakePhotoRepository : PhotoRepository {
        val savedPhotos = mutableListOf<PhotoMetadata>()
        override suspend fun getLocalPhotos(): List<PhotoMetadata> = savedPhotos
        override suspend fun getPhotoById(id: String): PhotoMetadata? = savedPhotos.find { it.id == id }
        override suspend fun savePhotoMetadata(metadata: PhotoMetadata) {
            savedPhotos.add(metadata)
        }
        override suspend fun photoExistsByUri(uri: String): Boolean = savedPhotos.any { it.localUri == uri }
    }

    @Test
    fun `scan empty directory returns zero`() = runTest {
        val useCase = ScanDirectoryUseCase(
            fileIO = FakeFileIO(emptyList()),
            exifParser = FakeExifParser(shouldSucceed = true),
            photoRepository = FakePhotoRepository()
        )
        val count = useCase("content://fake/dir")
        assertEquals(0, count)
    }

    @Test
    fun `scan directory with files returns correct count`() = runTest {
        val files = listOf("content://fake/1.jpg", "content://fake/2.jpg", "content://fake/3.jpg")
        val repo = FakePhotoRepository()
        val useCase = ScanDirectoryUseCase(
            fileIO = FakeFileIO(files),
            exifParser = FakeExifParser(shouldSucceed = true),
            photoRepository = repo
        )
        val count = useCase("content://fake/dir")
        assertEquals(3, count)
        assertEquals(3, repo.savedPhotos.size)
    }

    @Test
    fun `scan directory skips files with unparseable exif`() = runTest {
        val files = listOf("content://fake/corrupted.jpg")
        val repo = FakePhotoRepository()
        val useCase = ScanDirectoryUseCase(
            fileIO = FakeFileIO(files),
            exifParser = FakeExifParser(shouldSucceed = false),
            photoRepository = repo
        )
        val count = useCase("content://fake/dir")
        assertEquals(0, count)
        assertEquals(0, repo.savedPhotos.size)
    }
}
