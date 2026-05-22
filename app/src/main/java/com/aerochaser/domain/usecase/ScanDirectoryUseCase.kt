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
    suspend operator fun invoke(
        directoryUri: String,
        onProgress: (suspend (current: Int, total: Int) -> Unit)? = null
    ): Int {
        val files = fileIO.getFilesFromDirectory(directoryUri)
        val total = files.size
        var importedCount = 0

        for ((index, fileUri) in files.withIndex()) {
            onProgress?.invoke(index, total)
            if (photoRepository.photoExistsByUri(fileUri)) continue

            val photoId = UUID.randomUUID().toString()
            val metadata = exifParser.parseExif(fileUri, photoId)
            
            if (metadata != null) {
                photoRepository.savePhotoMetadata(metadata)
                importedCount++
            }
        }
        onProgress?.invoke(total, total)
        return importedCount
    }
}
