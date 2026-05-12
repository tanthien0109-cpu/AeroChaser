package com.aerochaser.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached AI-generated gear summaries. Keyed by the camera+lens combination
 * to avoid redundant API calls for the same hardware.
 */
@Entity(tableName = "ai_summaries")
data class AiSummaryEntity(
    @PrimaryKey val gearKey: String,
    val summary: String,
    val generatedAtMs: Long
)
