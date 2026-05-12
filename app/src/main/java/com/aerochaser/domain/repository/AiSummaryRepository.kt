package com.aerochaser.domain.repository

/**
 * Repository interface for generating and caching AI-powered gear descriptions.
 * Implementations should follow a tiered strategy:
 * 1. Check local cache
 * 2. Try on-device AI (Gemini Nano via AICore)
 * 3. Fall back to cloud AI (Gemini Flash)
 */
interface AiSummaryRepository {
    /**
     * Generates or retrieves a cached AI summary for the given camera+lens combination.
     * Only camera model and lens model strings are sent to AI — no other EXIF or PII.
     *
     * @return Result.success with the summary text, or Result.failure with a user-friendly message.
     */
    suspend fun generateSummary(cameraModel: String, lensModel: String): Result<String>
}
