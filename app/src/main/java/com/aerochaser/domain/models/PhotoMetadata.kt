package com.aerochaser.domain.models

/**
 * Domain model representing the EXIF and basic metadata of an aviation photo.
 * This is an explicit "seed" model designed to be easily portable to Swift/C#.
 */
data class PhotoMetadata(
    val id: String,
    val localUri: String,
    val captureDateMs: Long,
    val cameraModel: String?,
    val lensModel: String?,
    val aperture: String?,
    val shutterSpeed: String?,
    val iso: Int?,
    val focalLength: String?,
    val gpsLat: Double?,
    val gpsLng: Double?
)
