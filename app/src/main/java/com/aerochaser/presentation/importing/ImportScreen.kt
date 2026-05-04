package com.aerochaser.presentation.importing

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.aerochaser.data.local.worker.ImportWorker

@Composable
fun ImportScreen() {
    val context = LocalContext.current

    // PLATFORM-SPECIFIC: Android Storage Access Framework directory picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Trigger WorkManager
            val workManager = WorkManager.getInstance(context)
            val inputData = Data.Builder()
                .putString("DIRECTORY_URI", it.toString())
                .build()
                
            val importRequest = OneTimeWorkRequestBuilder<ImportWorker>()
                .setInputData(inputData)
                .build()
                
            workManager.enqueue(importRequest)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = { launcher.launch(null) }) {
            Text("Import Folder")
        }
    }
}
