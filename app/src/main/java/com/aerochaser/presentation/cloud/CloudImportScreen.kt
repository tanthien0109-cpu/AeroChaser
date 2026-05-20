package com.aerochaser.presentation.cloud

import android.app.Activity
import android.content.Context

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aerochaser.data.cloud.DriveFolder
import com.aerochaser.data.cloud.PhotosAlbum
import com.aerochaser.domain.models.PhotoMetadata
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import org.koin.androidx.compose.koinViewModel

private const val PHOTOS_SCOPE = "https://www.googleapis.com/auth/photoslibrary.readonly"

@Composable
fun CloudImportScreen(viewModel: CloudImportViewModel = koinViewModel()) {
    val authState by viewModel.authState.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .animateContentSize()
    ) {
        Text("Cloud Import", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Import photos from Google Drive & Photos", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        when (val state = authState) {
            is CloudAuthState.SignedOut -> SignedOutView(viewModel)
            is CloudAuthState.SigningIn -> LoadingView("Signing in…")
            is CloudAuthState.SignedIn -> {
                val email = state.email
                AccountBar(email, viewModel)
                Spacer(modifier = Modifier.height(12.dp))
                CloudTabBar(activeTab, onTabSelected = { viewModel.setActiveTab(it) })
                Spacer(modifier = Modifier.height(12.dp))
                when (activeTab) {
                    CloudTab.DRIVE -> DriveView(viewModel)
                    CloudTab.PHOTOS -> PhotosView(viewModel)
                }
            }
            is CloudAuthState.Error -> {
                ErrorCard(state.message) {}
                Spacer(modifier = Modifier.height(16.dp))
                SignedOutView(viewModel)
            }
        }
    }
}

@Composable
private fun SignedOutView(viewModel: CloudImportViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val acct = task.getResult(ApiException::class.java)
                val email = acct?.email ?: ""
                val account = acct?.account ?: android.accounts.Account(email, "com.google")
                viewModel.onSignInSuccess(account, email)
            } catch (e: ApiException) {
                Log.e("CloudImportScreen", "Sign-in failed", e)
                viewModel.onSignInFailed("Sign-in failed with code ${e.statusCode}: ${e.message}")
            }
        } else {
            viewModel.onSignInFailed("Sign-in was cancelled or failed.")
        }
    }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Cloud, contentDescription = "Cloud", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Connect your Google account to browse and import photos from Google Drive and Google Photos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            // P1 FIX: Request Drive AND Photos scopes at sign-in
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_READONLY), Scope(PHOTOS_SCOPE))
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            // Force re-consent to pick up new scopes
            client.signOut().addOnCompleteListener {
                launcher.launch(client.signInIntent)
            }
        }) {
            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect Google Account")
        }
    }
}

@Composable
private fun AccountBar(email: String, viewModel: CloudImportViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Signed in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(email, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { viewModel.signOut() }) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
            }
        }
    }
}

@Composable
private fun CloudTabBar(activeTab: CloudTab, onTabSelected: (CloudTab) -> Unit) {
    TabRow(selectedTabIndex = if (activeTab == CloudTab.DRIVE) 0 else 1) {
        Tab(selected = activeTab == CloudTab.DRIVE, onClick = { onTabSelected(CloudTab.DRIVE) }, text = { Text("Drive") }, icon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp)) })
        Tab(selected = activeTab == CloudTab.PHOTOS, onClick = { onTabSelected(CloudTab.PHOTOS) }, text = { Text("Photos") }, icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp)) })
    }
}

// ── Drive View ──────────────────────────────────────────────────

@Composable
private fun DriveView(viewModel: CloudImportViewModel) {
    val driveState by viewModel.driveState.collectAsState()
    when (driveState) {
        is DriveState.Idle -> {
            Button(onClick = { viewModel.loadDriveFolders() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Drive Folders")
            }
        }
        is DriveState.Loading -> LoadingView("Loading Drive…")
        is DriveState.FolderList -> {
            val state = driveState as DriveState.FolderList
            BreadcrumbRow(state.breadcrumbs, onNavigate = { viewModel.navigateToFolderBreadcrumb(it) })
            Spacer(modifier = Modifier.height(8.dp))
            if (state.folders.isEmpty()) {
                EmptyState("No folders found", "This folder is empty.")
            } else {
                LazyColumn {
                    items(state.folders, key = { it.id }) { folder ->
                        FolderItem(folder, onClick = { viewModel.navigateIntoFolder(folder.id, folder.name) }, onViewPhotos = { viewModel.loadDrivePhotosInFolder(folder.id, folder.name) })
                    }
                }
            }
        }
        is DriveState.PhotoList -> {
            val state = driveState as DriveState.PhotoList
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.folderName, style = MaterialTheme.typography.titleSmall)
                    Text("${state.photos.size} photos found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    TextButton(onClick = { viewModel.driveGoBack() }) { Text("Back") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.importDrivePhotos(state.photos) }, enabled = state.photos.isNotEmpty()) { Text("Import All") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (state.photos.isEmpty()) {
                EmptyState("No photos found", "This folder contains no image files.")
            } else {
                LazyColumn {
                    items(state.photos, key = { it.id }) { photo -> PhotoItem(photo) }
                }
            }
        }
        is DriveState.Importing -> {
            val state = driveState as DriveState.Importing
            ImportingView(state.current, state.total, state.skipped)
        }
        is DriveState.Complete -> {
            val state = driveState as DriveState.Complete
            CompleteView(state.imported, state.skipped, onBrowseMore = { viewModel.driveGoBack() })
        }
        is DriveState.Error -> {
            val state = driveState as DriveState.Error
            ErrorCard(state.message, onRetry = { viewModel.loadDriveFolders() })
        }
    }
}

// ── Photos View ─────────────────────────────────────────────────

@Composable
private fun PhotosView(viewModel: CloudImportViewModel) {
    val photosState by viewModel.photosState.collectAsState()
    when (photosState) {
        is PhotosState.Idle -> {
            Button(onClick = { viewModel.loadPhotosAlbums() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse Photo Albums")
            }
        }
        is PhotosState.Loading -> LoadingView("Loading Albums…")
        is PhotosState.AlbumList -> {
            val state = photosState as PhotosState.AlbumList
            if (state.albums.isEmpty()) {
                EmptyState("No albums found", "Your Google Photos library has no albums.")
            } else {
                Text("Your Albums", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.albums, key = { it.id }) { album -> AlbumCard(album, onClick = { viewModel.loadPhotosInAlbum(album.id, album.title) }) }
                }
            }
        }
        is PhotosState.PhotoGrid -> {
            val state = photosState as PhotosState.PhotoGrid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(state.albumTitle, style = MaterialTheme.typography.titleSmall)
                    Text("${state.photos.size} photos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    TextButton(onClick = { viewModel.photosGoBack() }) { Text("Back") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.importPhotosFromAlbum(state.photos) }, enabled = state.photos.isNotEmpty()) { Text("Import All") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (state.photos.isEmpty()) {
                EmptyState("No photos", "This album is empty.")
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.photos, key = { it.id }) { photo -> PhotoThumbnail(photo) }
                }
            }
        }
        is PhotosState.Importing -> {
            val state = photosState as PhotosState.Importing
            ImportingView(state.current, state.total, state.skipped)
        }
        is PhotosState.Complete -> {
            val state = photosState as PhotosState.Complete
            CompleteView(state.imported, state.skipped, onBrowseMore = { viewModel.photosGoBack() })
        }
        is PhotosState.Error -> {
            val state = photosState as PhotosState.Error
            ErrorCard(state.message, onRetry = { viewModel.loadPhotosAlbums() })
        }
    }
}

// ── Shared Components ───────────────────────────────────────────

@Composable
private fun BreadcrumbRow(breadcrumbs: List<FolderBreadcrumb>, onNavigate: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
        breadcrumbs.forEachIndexed { index, crumb ->
            if (index > 0) Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onNavigate(index) }, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(crumb.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (index == breadcrumbs.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FolderItem(folder: DriveFolder, onClick: () -> Unit, onViewPhotos: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick), shape = RoundedCornerShape(8.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(folder.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onViewPhotos) {
                Icon(Icons.Default.Photo, contentDescription = "View photos", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PhotoItem(photo: PhotoMetadata) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(6.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!photo.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(model = photo.thumbnailUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)), contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Icon(Icons.Default.Photo, contentDescription = null, modifier = Modifier.size(48.dp).padding(12.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(photo.fileName ?: "Unknown file", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                photo.cameraModel?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                photo.fileSizeBytes?.let { Text(formatFileSize(it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun AlbumCard(album: PhotosAlbum, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp)) {
        Column {
            if (album.coverPhotoUrl.isNotBlank()) {
                AsyncImage(model = album.coverPhotoUrl + "=w400-h300-c", contentDescription = album.title, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentScale = ContentScale.Crop)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoAlbum, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(album.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${album.itemCount} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: PhotoMetadata) {
    Card(shape = RoundedCornerShape(4.dp)) {
        if (!photo.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(model = photo.thumbnailUrl, contentDescription = photo.fileName, modifier = Modifier.aspectRatio(1f).fillMaxWidth(), contentScale = ContentScale.Crop)
        } else {
            Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Photo, contentDescription = null)
            }
        }
    }
}

@Composable
private fun LoadingView(message: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.FolderOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ImportingView(current: Int, total: Int, skipped: Int) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    Text("Importing… $current/$total", style = MaterialTheme.typography.titleSmall)
    if (skipped > 0) Text("$skipped duplicates skipped", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(12.dp))
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun CompleteView(imported: Int, skipped: Int, onBrowseMore: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Import Complete", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("$imported photos imported, $skipped skipped", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = onBrowseMore, modifier = Modifier.fillMaxWidth()) { Text("Browse More") }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Something went wrong", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

