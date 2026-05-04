package com.aerochaser.domain.exif

import com.aerochaser.domain.models.PhotoMetadata

/**
 * Interface seed for EXIF extraction.
 */
interface ExifParser {
    suspend fun parseExif(uri: String, photoId: String): PhotoMetadata?
}
