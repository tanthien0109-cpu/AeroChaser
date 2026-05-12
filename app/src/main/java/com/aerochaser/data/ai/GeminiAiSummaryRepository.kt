package com.aerochaser.data.ai

import android.util.Log
import com.aerochaser.data.local.dao.AiSummaryDao
import com.aerochaser.data.local.entity.AiSummaryEntity
import com.aerochaser.domain.repository.AiSummaryRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production implementation of [AiSummaryRepository].
 *
 * Tiered strategy:
 * 1. Room cache — instant, zero cost, zero network
 * 2. Gemini Flash (cloud) — reliable, fast, API key auth
 *
 * Privacy: ONLY camera model + lens model strings leave the device.
 * No GPS, no timestamps, no file paths, no user identifiers.
 */
class GeminiAiSummaryRepository(
    private val aiSummaryDao: AiSummaryDao,
    private val apiKey: String
) : AiSummaryRepository {

    companion object {
        private const val TAG = "GeminiAiSummary"
        private const val MODEL_NAME = "gemini-2.0-flash"
    }

    override suspend fun generateSummary(
        cameraModel: String,
        lensModel: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val gearKey = buildGearKey(cameraModel, lensModel)

        // ─── Tier 1: Room Cache ─────────────────────────────────────────
        try {
            val cached = aiSummaryDao.getSummary(gearKey)
            if (cached != null) {
                Log.d(TAG, "Cache hit for: $gearKey")
                return@withContext Result.success(cached.summary)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache read failed, continuing to API", e)
        }

        // ─── Tier 2: Gemini Flash (Cloud) ───────────────────────────────
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("AI overview is not available — API key not configured.")
            )
        }

        try {
            val model = GenerativeModel(
                modelName = MODEL_NAME,
                apiKey = apiKey
            )

            val prompt = buildPrompt(cameraModel, lensModel)
            Log.d(TAG, "Sending prompt to Gemini Flash for: $gearKey")

            val response = model.generateContent(prompt)
            val summaryText = response.text

            if (summaryText.isNullOrBlank()) {
                return@withContext Result.failure(
                    RuntimeException("AI returned an empty response. Try again later.")
                )
            }

            val trimmedSummary = summaryText.trim()

            // Cache the result for future use
            try {
                aiSummaryDao.insertSummary(
                    AiSummaryEntity(
                        gearKey = gearKey,
                        summary = trimmedSummary,
                        generatedAtMs = System.currentTimeMillis()
                    )
                )
                Log.d(TAG, "Cached summary for: $gearKey")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cache summary (non-critical)", e)
            }

            return@withContext Result.success(trimmedSummary)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Flash request failed for: $gearKey", e)
            return@withContext Result.failure(
                RuntimeException("AI overview is currently unavailable. Check your internet connection and try again.")
            )
        }
    }

    private fun buildGearKey(cameraModel: String, lensModel: String): String {
        return "${cameraModel.trim().lowercase()}|${lensModel.trim().lowercase()}"
    }

    private fun buildPrompt(cameraModel: String, lensModel: String): String {
        return """In 1-2 sentences, describe how the camera "$cameraModel" and lens "$lensModel" work together for photography. Focus on the practical strengths of this specific combination for capturing subjects like aircraft, wildlife, or sports."""
    }
}
