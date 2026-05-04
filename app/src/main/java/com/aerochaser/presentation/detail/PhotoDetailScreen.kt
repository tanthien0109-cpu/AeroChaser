package com.aerochaser.presentation.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var scale by remember { mutableFloatStateOf(1f) }
    var showMetadata by remember { mutableStateOf(true) }

    // Handle system back button
    BackHandler(onBack = onBack)

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showMetadata = !showMetadata }
    ) {
        AsyncImage(
            model = photo.localUri,
            contentDescription = "Full size aviation photo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
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
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "${photo.cameraModel ?: "Unknown Camera"} | ${photo.lensModel ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = buildExifString(photo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )

                // PLATFORM-SPECIFIC: Google Maps SDK placeholder integration
                if (photo.gpsLat != null && photo.gpsLng != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "📍 ${String.format("%.4f", photo.gpsLat)}, ${String.format("%.4f", photo.gpsLng)} (Map view coming soon)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // AI Tagging Stub
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🤖 ML Kit AI Tagging (Aircraft & Airline) — coming soon",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

private fun buildExifString(photo: PhotoMetadata): String {
    val parts = mutableListOf<String>()
    photo.aperture?.let { parts.add("f/$it") }
    photo.shutterSpeed?.let { parts.add("${it}s") }
    photo.iso?.let { parts.add("ISO $it") }
    photo.focalLength?.let { parts.add("${it}mm") }
    return if (parts.isEmpty()) "No EXIF data" else parts.joinToString(" • ")
}
