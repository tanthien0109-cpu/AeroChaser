package com.aerochaser.presentation.cloud

import android.accounts.Account
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import com.aerochaser.BuildConfig
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun CloudImportScreen(viewModel: CloudImportViewModel = koinViewModel()) {
    val authState by viewModel.authState.collectAsState()
    val importState by viewModel.importState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .animateContentSize()
    ) {
        // Header
        Text(
            text = "Cloud Import",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Import photos from Google Drive",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (authState) {
            is CloudAuthState.SignedOut -> SignedOutView(viewModel)
            is CloudAuthState.SigningIn -> LoadingView("Signing in…")
            is CloudAuthState.SignedIn -> {
                val email = (authState as CloudAuthState.SignedIn).email
                SignedInView(email, viewModel, importState)
            }
            is CloudAuthState.Error -> {
                ErrorView(
                    message = (authState as CloudAuthState.Error).message,
                    onRetry = { /* User taps sign in again */ }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SignedOutView(viewModel)
            }
        }
    }
}

@Composable
private fun SignedOutView(viewModel: CloudImportViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = "Cloud",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Connect your Google account to browse and import photos from Google Drive.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            scope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val request = viewModel.buildSignInRequest(BuildConfig.OAUTH_CLIENT_ID)
                    val result = credentialManager.getCredential(context as android.app.Activity, request)
                    val credential = result.credential

                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                        val email = googleIdToken.id
                        val account = Account(email, "com.google")
                        viewModel.onSignInSuccess(account, email)
                    } else {
                        viewModel.onSignInFailed("Unexpected credential type received.")
                    }
                } catch (e: Exception) {
                    Log.e("CloudImportScreen", "Sign-in failed", e)
                    viewModel.onSignInFailed(e.message ?: "Sign-in failed. Please try again.")
                }
            }
        }) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect Google Account")
        }
    }
}

@Composable
private fun SignedInView(email: String, viewModel: CloudImportViewModel, importState: ImportState) {
    // Account bar
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Signed in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(text = email, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { viewModel.signOut() }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Import state
    when (importState) {
        is ImportState.Idle -> {
            Button(onClick = { viewModel.loadFolders() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Drive Folders")
            }
        }

        is ImportState.Loading -> LoadingView("Loading…")

        is ImportState.FolderList -> {
            Text("Select a folder", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (importState.folders.isEmpty()) {
                Text("No folders found in Drive.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn {
                    items(importState.folders) { folder ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { viewModel.loadPhotosInFolder(folder.id, folder.name) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = folder.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }

        is ImportState.PhotoList -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(importState.folderName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${importState.photos.size} photos found",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    TextButton(onClick = { viewModel.goBack() }) { Text("Back") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.importPhotos(importState.photos) },
                        enabled = importState.photos.isNotEmpty()
                    ) { Text("Import All") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(importState.photos) { photo ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = photo.cameraModel ?: "Unknown camera",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (!photo.lensModel.isNullOrBlank()) {
                                    Text(
                                        text = photo.lensModel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        is ImportState.Importing -> {
            val progress = if (importState.total > 0) importState.current.toFloat() / importState.total else 0f
            Text(
                text = "Importing… ${importState.current}/${importState.total}",
                style = MaterialTheme.typography.titleSmall
            )
            if (importState.skipped > 0) {
                Text(
                    text = "${importState.skipped} duplicates skipped",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }

        is ImportState.Complete -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Import Complete",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${importState.imported} photos imported, ${importState.skipped} skipped",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { viewModel.goBack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Browse More Folders")
            }
        }

        is ImportState.Error -> {
            ErrorView(message = importState.message, onRetry = { viewModel.loadFolders() })
        }
    }
}

@Composable
private fun LoadingView(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Something went wrong",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}
