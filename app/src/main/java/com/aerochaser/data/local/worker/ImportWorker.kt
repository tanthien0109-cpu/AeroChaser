package com.aerochaser.data.local.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aerochaser.domain.usecase.ScanDirectoryUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ImportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    private val scanDirectoryUseCase: ScanDirectoryUseCase by inject()

    override suspend fun doWork(): Result {
        val directoryUri = inputData.getString("DIRECTORY_URI") ?: return Result.failure()

        return try {
            val importedCount = scanDirectoryUseCase(directoryUri)
            // Error handling for corrupted data is handled gracefully within the parser returning null
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
