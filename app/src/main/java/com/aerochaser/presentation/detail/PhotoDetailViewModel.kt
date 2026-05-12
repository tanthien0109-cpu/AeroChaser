package com.aerochaser.presentation.detail

import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aerochaser.domain.models.GearProfile
import com.aerochaser.domain.models.PhotoMetadata
import com.aerochaser.domain.repository.AiSummaryRepository
import com.aerochaser.domain.repository.GearInsightRepository
import com.aerochaser.domain.repository.PhotoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the state of the AI summary generation.
 */
sealed interface AiSummaryState {
    data object Idle : AiSummaryState
    data object Loading : AiSummaryState
    data class Success(val summary: String) : AiSummaryState
    data class Error(val message: String) : AiSummaryState
}

class PhotoDetailViewModel(
    private val photoRepository: PhotoRepository,
    private val gearInsightRepository: GearInsightRepository,
    private val aiSummaryRepository: AiSummaryRepository,
    private val geocoder: Geocoder
) : ViewModel() {

    // Full photo list for HorizontalPager navigation
    private val _allPhotos = MutableStateFlow<List<PhotoMetadata>>(emptyList())
    val allPhotos: StateFlow<List<PhotoMetadata>> = _allPhotos.asStateFlow()

    private val _initialPage = MutableStateFlow(0)
    val initialPage: StateFlow<Int> = _initialPage.asStateFlow()

    // Currently displayed photo (follows pager page changes)
    private val _currentPhoto = MutableStateFlow<PhotoMetadata?>(null)
    val currentPhoto: StateFlow<PhotoMetadata?> = _currentPhoto.asStateFlow()

    private val _gearProfile = MutableStateFlow<GearProfile?>(null)
    val gearProfile: StateFlow<GearProfile?> = _gearProfile.asStateFlow()

    private val _isGearLoading = MutableStateFlow(false)
    val isGearLoading: StateFlow<Boolean> = _isGearLoading.asStateFlow()

    private val _locationName = MutableStateFlow<String?>(null)
    val locationName: StateFlow<String?> = _locationName.asStateFlow()

    private val _aiSummaryState = MutableStateFlow<AiSummaryState>(AiSummaryState.Idle)
    val aiSummaryState: StateFlow<AiSummaryState> = _aiSummaryState.asStateFlow()

    /**
     * Loads all photos from the repository and sets the initial page index
     * to match the tapped photo's position. Called once on detail screen launch.
     */
    fun loadPhotos(initialPhotoId: String) {
        viewModelScope.launch {
            val photos = photoRepository.getLocalPhotos()
                .sortedByDescending { it.captureDateMs }
            _allPhotos.value = photos

            val index = photos.indexOfFirst { it.id == initialPhotoId }
            _initialPage.value = if (index >= 0) index else 0

            if (photos.isNotEmpty()) {
                onPageSettled(photos[_initialPage.value])
            }
        }
    }

    /**
     * Called when the pager settles on a new page. Updates the current photo
     * and fetches fresh gear insights and location name.
     */
    fun onPageSettled(photo: PhotoMetadata) {
        if (_currentPhoto.value?.id == photo.id) return

        _currentPhoto.value = photo
        _locationName.value = null
        _gearProfile.value = null
        _aiSummaryState.value = AiSummaryState.Idle

        fetchGearInsights(photo)
        if (photo.gpsLat != null && photo.gpsLng != null) {
            fetchLocationName(photo.gpsLat, photo.gpsLng)
        }
    }

    /**
     * Generates an AI summary for the current photo's camera+lens combination.
     * Called on-demand when the user expands the AI overview panel.
     * Only camera model and lens model strings are sent to the AI — no other data.
     */
    fun generateAiSummary() {
        val photo = _currentPhoto.value ?: return
        val camera = photo.cameraModel
        val lens = photo.lensModel

        // Cannot generate without at least one of camera or lens
        if (camera.isNullOrBlank() && lens.isNullOrBlank()) {
            _aiSummaryState.value = AiSummaryState.Error("No camera or lens information available for AI analysis.")
            return
        }

        // Don't re-generate if we already have a result for this photo
        if (_aiSummaryState.value is AiSummaryState.Success || _aiSummaryState.value is AiSummaryState.Loading) {
            return
        }

        _aiSummaryState.value = AiSummaryState.Loading

        viewModelScope.launch {
            val result = aiSummaryRepository.generateSummary(
                cameraModel = camera ?: "Unknown Camera",
                lensModel = lens ?: "Unknown Lens"
            )

            _aiSummaryState.value = result.fold(
                onSuccess = { AiSummaryState.Success(it) },
                onFailure = { AiSummaryState.Error(it.message ?: "AI overview generation failed.") }
            )
        }
    }

    private fun fetchLocationName(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                    _locationName.value = name
                }
            } catch (e: Exception) {
                // Geocoding failure is non-critical; the map still shows lat/lng
            }
        }
    }

    private fun fetchGearInsights(metadata: PhotoMetadata) {
        viewModelScope.launch {
            _isGearLoading.value = true
            val result = gearInsightRepository.fetchGearProfile(
                cameraModel = metadata.cameraModel,
                lensModel = metadata.lensModel,
                systemType = metadata.systemType
            )
            _gearProfile.value = result.getOrNull()
            _isGearLoading.value = false
        }
    }
}
