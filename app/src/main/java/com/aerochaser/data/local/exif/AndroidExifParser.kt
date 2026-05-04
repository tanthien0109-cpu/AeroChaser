package com.aerochaser.data.local.exif

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.aerochaser.domain.exif.ExifParser
import com.aerochaser.domain.models.PhotoMetadata
import java.text.SimpleDateFormat
import java.util.Locale

// PLATFORM-SPECIFIC: Android ExifInterface
class AndroidExifParser(private val context: Context) : ExifParser {

    companion object {
        private const val TAG = "AndroidExifParser"
        private val EXIF_DATE_FORMAT = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
    }

    override suspend fun parseExif(uriString: String, photoId: String): PhotoMetadata? {
        return try {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)

                val dateTimeStr = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exifInterface.getAttribute(ExifInterface.TAG_DATETIME)
                val captureDateMs = parseDateToMs(dateTimeStr)

                val cameraModel = exifInterface.getAttribute(ExifInterface.TAG_MODEL)
                val lensModel = exifInterface.getAttribute(ExifInterface.TAG_LENS_MODEL)
                val aperture = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
                val shutterSpeed = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                val iso = exifInterface.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, -1)
                    .takeIf { it > 0 }
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EXIF for uri=$uriString, photoId=$photoId", e)
            null
        }
    }

    private fun parseDateToMs(dateTimeStr: String?): Long {
        if (dateTimeStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            EXIF_DATE_FORMAT.parse(dateTimeStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse EXIF date: $dateTimeStr", e)
            System.currentTimeMillis()
        }
    }
}
