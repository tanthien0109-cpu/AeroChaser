package com.aerochaser.presentation.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aerochaser.domain.models.PhotoMetadata

@Composable
fun TimelineScreen(viewModel: TimelineViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is TimelineUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is TimelineUiState.Error -> {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is TimelineUiState.Success -> {
                if (state.photos.isEmpty()) {
                    Text(
                        text = "No photos imported yet. Let's add some planes!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 120.dp),
                        contentPadding = PaddingValues(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.photos, key = { it.id }) { photo ->
                            PhotoItem(photo = photo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoItem(photo: PhotoMetadata) {
    AsyncImage(
        model = photo.localUri,
        contentDescription = "Plane Photo ${photo.cameraModel}",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .fillMaxWidth()
    )
}
