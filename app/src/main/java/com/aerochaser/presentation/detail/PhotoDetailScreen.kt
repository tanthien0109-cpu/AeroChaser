@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.aerochaser.presentation.detail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.HardwareSystemType
import com.aerochaser.domain.models.PhotoMetadata
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.math.abs

// ─── Top-Level Screen ───────────────────────────────────────────────────────────

@Composable
fun PhotoDetailScreen(
    photos: List<PhotoMetadata>,
    initialPage: Int,
    currentPhoto: PhotoMetadata?,
    gearProfile: GearProfile?,
    isGearLoading: Boolean,
    locationName: String?,
    aiSummaryState: AiSummaryState,
    onPageSettled: (PhotoMetadata) -> Unit,
    onGenerateAiSummary: () -> Unit,
    onBack: () -> Unit
) {
    if (photos.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    BackHandler(onBack = onBack)

    var showPanel by remember { mutableStateOf(true) }
    var panelExpanded by remember { mutableStateOf(false) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { photos.size }
    )

    // Notify ViewModel when user settles on a new page
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page < photos.size) {
                onPageSettled(photos[page])
            }
        }
    }

    // Tabletop mode detection
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isTabletopMode = remember(activity) {
        if (activity == null) flowOf(false)
        else {
            WindowInfoTracker.getOrCreate(activity)
                .windowLayoutInfo(activity)
                .map { info ->
                    info.displayFeatures.filterIsInstance<FoldingFeature>().any { feature ->
                        feature.state == FoldingFeature.State.HALF_OPENED &&
                            feature.orientation == FoldingFeature.Orientation.HORIZONTAL
                    }
                }
        }
    }.collectAsState(initial = false).value

    if (isTabletopMode) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Top Half: Horizontal Photo Pager
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentPageZoomed,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val photo = photos[page]
                    val isSettled = pagerState.settledPage == page

                    PhotoPage(
                        photo = photo,
                        isActive = isSettled,
                        onTap = {}, // tap to toggle panel disabled in tabletop split layout
                        onZoomChanged = { zoomed ->
                            if (isSettled) isCurrentPageZoomed = zoomed
                        }
                    )
                }

                // Page Indicator overlay
                if (photos.size > 1) {
                    Text(
                        text = "${pagerState.settledPage + 1} / ${photos.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // High-fidelity Crease Divider (simulating a clean crease line)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.DarkGray)
            )

            // Bottom Half: Scrollable Metadata & Console Panel (always expanded)
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .background(Color.Black)
                    .verticalScroll(rememberScrollState())
            ) {
                currentPhoto?.let { photo ->
                    MetadataPanel(
                        photo = photo,
                        gearProfile = gearProfile,
                        isGearLoading = isGearLoading,
                        locationName = locationName,
                        aiSummaryState = aiSummaryState,
                        expanded = true, // permanently open in split layout for desk viewing
                        onToggleExpand = {}, // expand toggle disabled in split tabletop view
                        onGenerateAiSummary = onGenerateAiSummary
                    )
                }
            }
        }
    } else {
        // Standard full-screen layout with overlapping bottom panel
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // ─── Horizontal Photo Pager ─────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                val isSettled = pagerState.settledPage == page

                PhotoPage(
                    photo = photo,
                    isActive = isSettled,
                    onTap = { showPanel = !showPanel },
                    onZoomChanged = { zoomed ->
                        if (isSettled) isCurrentPageZoomed = zoomed
                    }
                )
            }

            // ─── Page Indicator ─────────────────────────────────────────────
            if (photos.size > 1) {
                Text(
                    text = "${pagerState.settledPage + 1} / ${photos.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // ─── Bottom Metadata Panel ──────────────────────────────────────
            AnimatedVisibility(
                visible = showPanel && currentPhoto != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                currentPhoto?.let { photo ->
                    MetadataPanel(
                        photo = photo,
                        gearProfile = gearProfile,
                        isGearLoading = isGearLoading,
                        locationName = locationName,
                        aiSummaryState = aiSummaryState,
                        expanded = panelExpanded,
                        onToggleExpand = { panelExpanded = !panelExpanded },
                        onGenerateAiSummary = onGenerateAiSummary
                    )
                }
            }
        }
    }
}

// ─── Single Photo Page with Gestures ────────────────────────────────────────────

@Composable
private fun PhotoPage(
    photo: PhotoMetadata,
    isActive: Boolean,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var loadRetryKey by remember { mutableIntStateOf(0) }
    var imageState by remember { mutableStateOf<AsyncImagePainter.State?>(null) }

    // Animated zoom for double-tap transitions
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "zoom"
    )
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "panX"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "panY"
    )

    // Report zoom state to parent for pager control
    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    // Reset zoom when swiping away from this page
    LaunchedEffect(isActive) {
        if (!isActive && scale != 1f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        }
    }

    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo.localUri + "?retry=$loadRetryKey",
            contentDescription = "Aviation photo",
            contentScale = ContentScale.Fit,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                // Reset to 1x
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                // Zoom to 3x
                                scale = 3f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale

                        if (newScale > 1.05f) {
                            // Pan when zoomed — constrain to image bounds
                            val maxX = screenWidthPx * (newScale - 1) / 2
                            val maxY = screenHeightPx * (newScale - 1) / 2
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = animatedOffsetX,
                    translationY = animatedOffsetY
                )
        )

        // ─── Error Overlay ──────────────────────────────────────────────
        val errorState = imageState
        if (errorState is AsyncImagePainter.State.Error) {
            ErrorOverlay(
                errorMessage = errorState.result.throwable.localizedMessage ?: "Unknown error",
                photoUri = photo.localUri,
                onRetry = { loadRetryKey++ }
            )
        }
    }
}

// ─── Metadata Panel ─────────────────────────────────────────────────────────────

@Composable
private fun MetadataPanel(
    photo: PhotoMetadata,
    gearProfile: GearProfile?,
    isGearLoading: Boolean,
    locationName: String?,
    aiSummaryState: AiSummaryState,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onGenerateAiSummary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.8f))
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = 24.dp)
    ) {
        // ─── Header Row (always visible — camera + lens + expand toggle) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isGearLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    GearHeader(photo, gearProfile)
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }

        // ─── Expanded Content ───────────────────────────────────────────
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))

            // Gear details
            if (gearProfile != null && !isGearLoading) {
                GearDetails(gearProfile)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Exposure row
            ExposureRow(photo)

            // AI Overview (collapsible, generates on-demand)
            if (!photo.cameraModel.isNullOrBlank() || !photo.lensModel.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                AiOverviewPanel(
                    state = aiSummaryState,
                    onGenerate = onGenerateAiSummary
                )
            }

            // Location
            if (photo.gpsLat != null && photo.gpsLng != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LocationSection(photo.gpsLat, photo.gpsLng, locationName)
            }
        }
    }
}

// ─── Gear Components ────────────────────────────────────────────────────────────

@Composable
private fun GearHeader(photo: PhotoMetadata, gearProfile: GearProfile?) {
    val typeLabel = when (gearProfile?.type) {
        HardwareSystemType.INTEGRATED -> "📱 Integrated System"
        HardwareSystemType.INTERCHANGEABLE -> "📷 Interchangeable System"
        else -> null
    }

    if (typeLabel != null) {
        Text(
            text = typeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }

    Text(
        text = photo.cameraModel ?: "Unknown Camera",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    if (!photo.lensModel.isNullOrBlank() && photo.lensModel != photo.cameraModel) {
        Text(
            text = photo.lensModel,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray
        )
    }
}

@Composable
private fun GearDetails(profile: GearProfile) {
    if (profile.summary.isNotBlank()) {
        Text(
            text = profile.summary,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray.copy(alpha = 0.8f)
        )
    }

    when (profile.type) {
        HardwareSystemType.INTEGRATED -> {
            profile.bodyDetails?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "System: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        HardwareSystemType.INTERCHANGEABLE -> {
            profile.bodyDetails?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Body: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            profile.lensDetails?.let {
                Text(text = "Lens: $it", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        HardwareSystemType.UNKNOWN -> {
            profile.bodyDetails?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

// ─── Exposure Row ───────────────────────────────────────────────────────────────

@Composable
private fun ExposureRow(photo: PhotoMetadata) {
    val parts = mutableListOf<String>()
    photo.focalLength?.let { parts.add("${it}mm") }
    photo.aperture?.let { parts.add("f/$it") }
    photo.shutterSpeed?.let { parts.add("${it}s") }
    photo.iso?.let { parts.add("ISO $it") }

    Text(
        text = if (parts.isEmpty()) "No metadata available" else parts.joinToString(" • "),
        style = MaterialTheme.typography.bodyMedium,
        color = if (parts.isEmpty()) Color.Gray else MaterialTheme.colorScheme.primaryContainer
    )
}

// ─── Location Section ───────────────────────────────────────────────────────────

@Composable
private fun LocationSection(lat: Double, lng: Double, locationName: String?) {
    val locationText = if (locationName != null) {
        "📍 $locationName (${String.format("%.4f", lat)}, ${String.format("%.4f", lng)})"
    } else {
        "📍 ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}"
    }

    Text(
        text = locationText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(8.dp))

    val position = LatLng(lat, lng)
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(position, 12f)
    }

    LaunchedEffect(lat, lng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(position, 12f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                scrollGesturesEnabled = false,
                zoomGesturesEnabled = false,
                tiltGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        ) {
            Marker(state = MarkerState(position = position))
        }
    }
}

// ─── Error Overlay ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorOverlay(errorMessage: String, photoUri: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.padding(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "This photo couldn't be loaded",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The original file may have been moved, deleted, or is in an unsupported format.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Path: ${photoUri.takeLast(60)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Error: $errorMessage",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry")
            }
        }
    }
}

// ─── AI Overview Panel ──────────────────────────────────────────────────────────

@Composable
private fun AiOverviewPanel(state: AiSummaryState, onGenerate: () -> Unit) {
    var aiExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    aiExpanded = !aiExpanded
                    if (aiExpanded) onGenerate()
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✨ AI Overview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (aiExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (aiExpanded) "Collapse AI" else "Expand AI",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(visible = aiExpanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                when (state) {
                    is AiSummaryState.Idle -> {
                        Text(
                            text = "Tap to generate an AI description of this gear combination.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    is AiSummaryState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Generating overview…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }
                    }
                    is AiSummaryState.Success -> {
                        Text(
                            text = state.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    is AiSummaryState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
