package com.aerochaser.domain.repository

import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.HardwareSystemType

/**
 * Interface seed for fetching contextual insights about camera gear.
 */
interface GearInsightRepository {
    
    /**
     * Fetches a generated profile for the provided hardware.
     * Implementations must handle offline states and missing data gracefully,
     * ensuring a [Result] is returned rather than crashing.
     */
    suspend fun fetchGearProfile(
        cameraModel: String?, 
        lensModel: String?, 
        systemType: HardwareSystemType
    ): Result<GearProfile>
}
