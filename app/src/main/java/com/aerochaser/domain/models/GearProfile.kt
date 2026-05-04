package com.aerochaser.domain.models

/**
 * Represents contextual insights about the imaging hardware used for a photo.
 * This can be sourced locally via heuristics or remotely via an AI/Web API.
 */
data class GearProfile(
    val type: HardwareSystemType,
    val summary: String,
    val bodyDetails: String?,
    val lensDetails: String?
)
