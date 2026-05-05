package com.aerochaser.presentation.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
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
import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.HardwareSystemType
import com.aerochaser.domain.models.PhotoMetadata

@Composable
fun PhotoDetailScreen(
    photo: PhotoMetadata,
    gearProfile: GearProfile?,
    isGearLoading: Boolean,
    locationName: String?,
    onBack: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var showMetadata by remember { mutableStateOf(true) }

    BackHandler(onBack = onBack)

    Box(
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
                .graphicsLayer(scaleX = scale, scaleY = scale)
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
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                if (isGearLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (gearProfile != null) {
                    when (gearProfile.type) {
                        HardwareSystemType.INTEGRATED -> IntegratedSystemOverlay(photo, gearProfile)
                        HardwareSystemType.INTERCHANGEABLE -> InterchangeableSystemOverlay(photo, gearProfile)
                        HardwareSystemType.UNKNOWN -> GenericSystemOverlay(photo, gearProfile)
                    }
                } else {
                    // Graceful fallback if gear profile completely failed
                    GenericSystemOverlay(photo, null)
                }

                // GPS & Future hooks
                if (photo.gpsLat != null && photo.gpsLng != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val locationText = if (locationName != null) {
                        "📍 $locationName (${String.format("%.4f", photo.gpsLat)}, ${String.format("%.4f", photo.gpsLng)})"
                    } else {
                        "📍 ${String.format("%.4f", photo.gpsLat)}, ${String.format("%.4f", photo.gpsLng)}"
                    }
                    
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        com.google.maps.android.compose.GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = com.google.maps.android.compose.CameraPositionState(
                                position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(
                                    com.google.android.gms.maps.model.LatLng(photo.gpsLat, photo.gpsLng),
                                    12f
                                )
                            ),
                            uiSettings = com.google.maps.android.compose.MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
                        ) {
                            com.google.maps.android.compose.Marker(
                                state = com.google.maps.android.compose.MarkerState(position = com.google.android.gms.maps.model.LatLng(photo.gpsLat, photo.gpsLng))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntegratedSystemOverlay(photo: PhotoMetadata, profile: GearProfile) {
    Column {
        Text(
            text = "📱 Integrated Imaging System",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = photo.cameraModel ?: "Unknown Device",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = profile.summary,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Emphasize the full array vs just the lens used
        Text(
            text = "Array Specs: ${profile.bodyDetails ?: "Unknown"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        ExposureRow(photo)
    }
}

@Composable
private fun InterchangeableSystemOverlay(photo: PhotoMetadata, profile: GearProfile) {
    Column {
        Text(
            text = "📷 Interchangeable System",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        
        // Body
        Text(
            text = "Body: ${profile.bodyDetails ?: photo.cameraModel ?: "Unknown Body"}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        // Lens
        Text(
            text = "Lens: ${profile.lensDetails ?: photo.lensModel ?: "Unknown Lens"}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = profile.summary,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        ExposureRow(photo)
    }
}

@Composable
private fun GenericSystemOverlay(photo: PhotoMetadata, profile: GearProfile?) {
    Column {
        Text(
            text = "${photo.cameraModel ?: "Unknown Camera"} | ${photo.lensModel ?: ""}",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        if (profile != null) {
            Text(
                text = profile.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        ExposureRow(photo)
    }
}

@Composable
private fun ExposureRow(photo: PhotoMetadata) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val parts = mutableListOf<String>()
        photo.focalLength?.let { parts.add("${it}mm") }
        photo.aperture?.let { parts.add("f/$it") }
        photo.shutterSpeed?.let { parts.add("${it}s") }
        photo.iso?.let { parts.add("ISO $it") }
        
        Text(
            text = if (parts.isEmpty()) "No EXIF data" else parts.joinToString(" • "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
