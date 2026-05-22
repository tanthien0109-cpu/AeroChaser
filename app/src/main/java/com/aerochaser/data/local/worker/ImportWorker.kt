package com.aerochaser.data.local.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.aerochaser.domain.usecase.ScanDirectoryUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    companion object {
        private const val TAG = "ImportWorker"
        const val KEY_DIRECTORY_URI = "DIRECTORY_URI"
        const val KEY_IMPORTED_COUNT = "IMPORTED_COUNT"
        const val KEY_ERROR_MESSAGE = "ERROR_MESSAGE"
    }

    private val scanDirectoryUseCase: ScanDirectoryUseCase by inject()

    override suspend fun doWork(): Result {
        val directoryUri = inputData.getString(KEY_DIRECTORY_URI)
        if (directoryUri.isNullOrBlank()) {
            Log.e(TAG, "No directory URI provided in input data")
            return Result.failure(
                Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, "No directory URI provided")
                    .build()
            )
        }

        Log.i(TAG, "Starting import from: $directoryUri")

        return try {
            val importedCount = scanDirectoryUseCase(directoryUri) { current, total ->
                setProgress(
                    Data.Builder()
                        .putInt("CURRENT", current)
                        .putInt("TOTAL", total)
                        .build()
                )
            }
            Log.i(TAG, "Import complete: $importedCount photos imported from $directoryUri")
            Result.success(
                Data.Builder()
                    .putInt(KEY_IMPORTED_COUNT, importedCount)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Import failed for directory: $directoryUri", e)
            Result.failure(
                Data.Builder()
                    .putString(KEY_ERROR_MESSAGE, e.message ?: "Unknown import error")
                    .build()
            )
        }
    }
}
