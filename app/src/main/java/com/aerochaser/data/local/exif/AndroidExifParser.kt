package com.aerochaser.data.local.exif

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.aerochaser.domain.exif.ExifParser
import com.aerochaser.domain.models.PhotoMetadata
import java.io.InputStream

// PLATFORM-SPECIFIC: Android ExifInterface
class AndroidExifParser(private val context: Context) : ExifParser {
    override suspend fun parseExif(uriString: String, photoId: String): PhotoMetadata? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            val exifInterface = ExifInterface(inputStream)
            inputStream.close()

            // Extract metadata
            val dateTimeStr = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            // Need to parse dateTimeStr to milliseconds (simplified for this mock)
            val captureDateMs = System.currentTimeMillis() // TODO: parse actual date format "yyyy:MM:dd HH:mm:ss"

            val cameraModel = exifInterface.getAttribute(ExifInterface.TAG_MODEL)
            val lensModel = exifInterface.getAttribute(ExifInterface.TAG_LENS_MODEL)
            val aperture = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
            val shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
            val iso = exifInterface.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, -1).takeIf { it != -1 }
            val focalLength = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)

            val latLong = exifInterface.latLong
            val lat = latLong?.get(0)
            val lng = latLong?.get(1)

            PhotoMetadata(
                id = photoId,
                localUri = uriString,
                captureDateMs = captureDateMs,
                cameraModel = cameraModel,
                lensModel = lensModel,
                aperture = aperture,
                shutterSpeed = shutterSpeed,
                iso = iso,
                focalLength = focalLength,
                gpsLat = lat,
                gpsLng = lng
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
