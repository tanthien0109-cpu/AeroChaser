package com.aerochaser.domain.usecase

import com.aerochaser.domain.models.HardwareSystemType

/**
 * Intelligent heuristics engine to classify hardware architectures based on raw EXIF strings.
 * Operates under a Nine Zeros reliability standard, expecting fragmented and corrupted inputs.
 */
object HardwareClassifier {

    private val SMARTPHONE_BRANDS = setOf(
        "apple", "iphone", "samsung", "galaxy", "xiaomi", "redmi", "poco",
        "google", "pixel", "oneplus", "vivo", "oppo", "huawei", "honor",
        "motorola", "moto", "sony ericsson", "nokia", "asus", "rog", "xperia"
    )

    private val DEDICATED_CAMERA_BRANDS = setOf(
        "nikon", "canon", "sony", "fujifilm", "fuji", "olympus",
        "panasonic", "lumix", "pentax", "leica", "hasselblad", "ricoh"
    )

    private val DRONE_BRANDS = setOf(
        "dji", "mavic", "phantom", "autel", "skydio", "parrot", "yuneec"
    )

    /**
     * Classifies the imaging hardware architecture.
     *
     * @param cameraModel The raw EXIF TAG_MODEL string.
     * @param lensModel The raw EXIF TAG_LENS_MODEL string.
     * @return The computed [HardwareSystemType].
     */
    fun classify(cameraModel: String?, lensModel: String?): HardwareSystemType {
        val cam = cameraModel?.trim()?.lowercase() ?: ""
        val lens = lensModel?.trim()?.lowercase() ?: ""

        // Edge Case: Absolute missing data
        if (cam.isBlank() && lens.isBlank()) {
            return HardwareSystemType.UNKNOWN
        }

        // 1. Explicit Drone Checks (Integrated)
        if (DRONE_BRANDS.any { cam.contains(it) }) {
            return HardwareSystemType.INTEGRATED
        }

        // 2. Explicit Smartphone Checks (Integrated)
        // Check camera model against known mobile brands
        if (SMARTPHONE_BRANDS.any { cam.contains(it) }) {
            return HardwareSystemType.INTEGRATED
        }
        
        // Sometimes the lens string gives it away even if the camera string is weird
        if (SMARTPHONE_BRANDS.any { lens.contains(it) } || lens.contains("rear camera") || lens.contains("front camera")) {
            return HardwareSystemType.INTEGRATED
        }

        // 3. Interchangeable Checks
        // Requires a dedicated camera brand AND a distinctly different lens string.
        val isDedicatedBrand = DEDICATED_CAMERA_BRANDS.any { cam.contains(it) }
        
        if (isDedicatedBrand) {
            // Some compact point-and-shoots (like Sony RX100, Fuji X100) are dedicated brands but Integrated.
            // They typically don't report a distinct TAG_LENS_MODEL, or they report something that matches the camera.
            if (lens.isNotBlank() && cam != lens && !lens.contains("built-in")) {
                return HardwareSystemType.INTERCHANGEABLE
            } else {
                // If there's no distinct lens string, it's likely a fixed-lens compact camera.
                return HardwareSystemType.INTEGRATED
            }
        }

        // 4. Fallback Heuristics
        // If we don't recognize the brand, look at the structural relationship.
        if (lens.isBlank()) {
            return HardwareSystemType.INTEGRATED // No lens data implies it's inseparable in EXIF
        }
        
        if (lens.contains("built-in") || lens.contains("integrated")) {
            return HardwareSystemType.INTEGRATED
        }

        if (cam.isNotBlank() && lens.isNotBlank() && cam != lens) {
             // We have two distinct, non-matching strings but don't recognize the brand. 
             // Statistically, this represents an interchangeable setup.
             return HardwareSystemType.INTERCHANGEABLE
        }

        return HardwareSystemType.UNKNOWN
    }
}
