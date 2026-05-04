package com.aerochaser.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aerochaser.domain.models.PhotoMetadata

@Composable
fun PhotoDetailScreen(photo: PhotoMetadata, onBack: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var showMetadata by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showMetadata = !showMetadata }
    ) {
        AsyncImage(
            model = photo.localUri,
            contentDescription = "Full Size Photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 3f)
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale
                )
        )

        AnimatedVisibility(
            visible = showMetadata,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp)
                    .padding(bottom = 32.dp) // Edge to edge padding
            ) {
                Text(
                    text = "${photo.cameraModel ?: "Unknown Camera"} | ${photo.lensModel ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = "f/${photo.aperture ?: "-"} • 1/${photo.shutterSpeed ?: "-"}s • ISO ${photo.iso ?: "-"} • ${photo.focalLength ?: "-"}mm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
                
                // PLATFORM-SPECIFIC: Google Maps SDK placeholder integration
                if (photo.gpsLat != null && photo.gpsLng != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📍 Location: ${photo.gpsLat}, ${photo.gpsLng} (Map view coming soon)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
