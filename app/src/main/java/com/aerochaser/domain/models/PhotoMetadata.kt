package com.aerochaser.domain.models

/**
 * Defines the physical architecture of the imaging hardware.
 */
enum class HardwareSystemType {
    /**
     * A system where the lens and sensor are a single, indivisible unit.
     * Examples: Smartphones (Xiaomi 13 Ultra, iPhone 15 Pro), Point-and-shoot compacts, Drones.
     */
    INTEGRATED,

    /**
     * A system where the lens can be detached and swapped on the camera body.
     * Examples: DSLRs (Nikon D500), Mirrorless bodies (Sony A7IV).
     */
    INTERCHANGEABLE,

    /**
     * Hardware type cannot be confidently determined due to missing, generic, or corrupted EXIF data.
     */
    UNKNOWN
}

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
    val gpsLng: Double?,
    val systemType: HardwareSystemType = HardwareSystemType.UNKNOWN,
    // Cloud metadata fields
    val fileName: String? = null,
    val fileSizeBytes: Long? = null,
    val modifiedDateMs: Long? = null,
    val thumbnailUrl: String? = null
)
