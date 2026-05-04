package com.aerochaser.data.repository

import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.HardwareSystemType
import com.aerochaser.domain.repository.GearInsightRepository
import kotlinx.coroutines.delay

/**
 * A highly reliable stub implementation of the GearInsightRepository.
 * Simulates a robust AI/Web API fetch with predefined profiles for testing
 * and graceful fallbacks for unknown configurations.
 */
class GearInsightRepositoryStub : GearInsightRepository {

    override suspend fun fetchGearProfile(
        cameraModel: String?,
        lensModel: String?,
        systemType: HardwareSystemType
    ): Result<GearProfile> {
        
        // Simulate network latency (between 100ms and 500ms)
        delay((100L..500L).random())

        val cam = cameraModel?.trim()?.lowercase() ?: ""
        val lens = lensModel?.trim()?.lowercase() ?: ""

        // Handle absolute missing data (Edge Case Defense)
        if (cam.isBlank() && lens.isBlank()) {
            return Result.failure(IllegalArgumentException("No hardware metadata available for insight lookup."))
        }

        // Profile A Validation (Integrated System - Xiaomi 13 Ultra)
        if (cam.contains("xiaomi") && cam.contains("13") && cam.contains("ultra")) {
            return Result.success(
                GearProfile(
                    type = HardwareSystemType.INTEGRATED,
                    summary = "A flagship mobile photography powerhouse engineered with Leica.",
                    bodyDetails = "Features a 1-inch type Sony IMX989 main sensor and variable aperture, alongside three IMX858 sensors covering ultrawide, telephoto, and periscope focal lengths.",
                    lensDetails = "Leica Vario-Summicron 1:1.8-3.0/12-120 ASPH integrated array."
                )
            )
        }

        // Profile B Validation (Interchangeable System - Nikon D500 + 80-400)
        if (cam.contains("nikon") && cam.contains("d500") && lens.contains("80-400")) {
            return Result.success(
                GearProfile(
                    type = HardwareSystemType.INTERCHANGEABLE,
                    summary = "A legendary pro-tier APS-C DSLR paired with extreme telephoto reach.",
                    bodyDetails = "Nikon D500: Renowned for its 153-point AF system and 10fps burst, making it a gold standard for aviation and wildlife.",
                    lensDetails = "AF-S NIKKOR 80-400mm f/4.5-5.6G ED VR: Offers a massive 120-600mm equivalent field of view on the D500's DX sensor."
                )
            )
        }

        // Graceful Fallback for general systems
        val profile = when (systemType) {
            HardwareSystemType.INTEGRATED -> {
                val name = if (cam.isNotBlank()) cameraModel else "Integrated Camera Device"
                GearProfile(
                    type = HardwareSystemType.INTEGRATED,
                    summary = "Integrated mobile or fixed-lens imaging system.",
                    bodyDetails = "Hardware profile generated for $name.",
                    lensDetails = "Uses a built-in optical assembly."
                )
            }
            HardwareSystemType.INTERCHANGEABLE -> {
                val bName = if (cam.isNotBlank()) cameraModel else "Unknown Body"
                val lName = if (lens.isNotBlank()) lensModel else "Unknown Lens"
                GearProfile(
                    type = HardwareSystemType.INTERCHANGEABLE,
                    summary = "Interchangeable lens imaging system.",
                    bodyDetails = bName,
                    lensDetails = lName
                )
            }
            HardwareSystemType.UNKNOWN -> {
                val identifier = sequenceOf(cameraModel, lensModel).filterNotNull().firstOrNull { it.isNotBlank() } ?: "Unknown Device"
                GearProfile(
                    type = HardwareSystemType.UNKNOWN,
                    summary = "Hardware classification indeterminate.",
                    bodyDetails = "Raw identifier: $identifier",
                    lensDetails = null
                )
            }
        }

        return Result.success(profile)
    }
}
