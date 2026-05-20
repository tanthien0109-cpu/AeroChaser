package com.aerochaser.domain.usecase

import com.aerochaser.domain.exif.ExifParser
import com.aerochaser.domain.io.FileIO
import com.aerochaser.domain.repository.PhotoRepository
import java.util.UUID

class ScanDirectoryUseCase(
    private val fileIO: FileIO,
    private val exifParser: ExifParser,
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(directoryUri: String): Int {
        val files = fileIO.getFilesFromDirectory(directoryUri)
        var importedCount = 0

        for (fileUri in files) {
            if (photoRepository.photoExistsByUri(fileUri)) continue

            val photoId = UUID.randomUUID().toString()
            val metadata = exifParser.parseExif(fileUri, photoId)
            
            if (metadata != null) {
                photoRepository.savePhotoMetadata(metadata)
                importedCount++
            }
        }
        return importedCount
    }
}
